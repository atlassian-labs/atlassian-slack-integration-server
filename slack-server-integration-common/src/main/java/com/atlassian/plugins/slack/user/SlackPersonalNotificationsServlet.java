package com.atlassian.plugins.slack.user;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugins.slack.admin.AbstractPermissionCheckingServlet;
import com.atlassian.plugins.slack.admin.LoginRedirectionManager;
import com.atlassian.plugins.slack.analytics.AnalyticsContextProvider;
import com.atlassian.plugins.slack.api.client.SlackClient;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.atlassian.plugins.slack.api.events.PageVisitedEvent;
import com.atlassian.plugins.slack.api.events.PageVisitedEvent.CommonPage;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.plugins.slack.rest.model.LimitedSlackLinkDto;
import com.atlassian.plugins.slack.settings.SlackUserSettingsService;
import com.atlassian.plugins.slack.spi.SlackPluginResourceProvider;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.UrlMode;
import com.atlassian.sal.api.user.UserKey;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.soy.renderer.SoyException;
import com.atlassian.soy.renderer.SoyTemplateRenderer;
import com.atlassian.webresource.api.assembler.PageBuilderService;
import io.atlassian.fugue.Either;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.UriBuilder;
import org.springframework.beans.factory.annotation.Qualifier;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Displays a page showing the user personal notification settings. It renders configurations dynamically for each
 * one of the plugins.
 */
public class SlackPersonalNotificationsServlet extends AbstractPermissionCheckingServlet {
    private final ApplicationProperties applicationProperties;
    private final SoyTemplateRenderer soyTemplateRenderer;
    private final PageBuilderService pageBuilderService;
    private final SlackPluginResourceProvider slackPluginResourceProvider;
    private final SlackUserSettingsService slackUserSettingsService;
    private final SlackLinkManager slackLinkManager;
    private final SlackClientProvider slackClientProvider;
    private final EventPublisher eventPublisher;
    private final AnalyticsContextProvider analyticsContextProvider;

    public SlackPersonalNotificationsServlet(
            @Qualifier("salUserManager") final UserManager userManager,
            @Qualifier("salApplicationProperties") final ApplicationProperties applicationProperties,
            final LoginRedirectionManager loginRedirectionManager,
            final SoyTemplateRenderer soyTemplateRenderer,
            final PageBuilderService pageBuilderService,
            final SlackPluginResourceProvider slackPluginResourceProvider,
            final SlackUserSettingsService slackUserSettingsService,
            final SlackLinkManager slackLinkManager,
            final SlackClientProvider slackClientProvider,
            final EventPublisher eventPublisher,
            final AnalyticsContextProvider analyticsContextProvider) {
        super(userManager, loginRedirectionManager);
        this.applicationProperties = applicationProperties;
        this.soyTemplateRenderer = soyTemplateRenderer;
        this.pageBuilderService = pageBuilderService;
        this.slackPluginResourceProvider = slackPluginResourceProvider;
        this.slackUserSettingsService = slackUserSettingsService;
        this.slackLinkManager = slackLinkManager;
        this.slackClientProvider = slackClientProvider;
        this.eventPublisher = eventPublisher;
        this.analyticsContextProvider = analyticsContextProvider;
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        pageBuilderService.assembler().resources().requireWebResource(slackPluginResourceProvider.getPluginKey() + ":slack-personal-settings-resources");
        response.setContentType("text/html;charset=UTF-8");

        final UserKey userKey = userManager.getRemoteUser().getUserKey();

        final List<String> notificationKeys = slackPluginResourceProvider.getPersonalConfigurationKeys()
                .stream()
                .sorted()
                .map(Enum::name)
                .map(String::toLowerCase)
                .collect(Collectors.toList());
        final Map<String, Boolean> notificationMap = slackPluginResourceProvider.getPersonalConfigurationKeys()
                .stream()
                .collect(Collectors.toMap(
                        key -> key.name().toLowerCase(),
                        key -> slackUserSettingsService.isPersonalNotificationTypeEnabled(userKey, key)));

        final Map<String, Object> context = new HashMap<>();
        context.put("notificationMap", notificationMap);
        context.put("notificationKeys", notificationKeys);

        final String teamId = slackUserSettingsService.getNotificationTeamId(userKey);
        if (teamId != null) {
            context.put("teamId", teamId);
        }

        context.put("links", slackLinkManager.getLinks().stream()
                .filter(link -> slackClientProvider.withLink(link)
                        .withRemoteUserTokenIfAvailable()
                        .map(SlackClient::testToken)
                        .filter(Either::isRight)
                        .isPresent())
                .map(LimitedSlackLinkDto::new)
                .collect(Collectors.toList()));

        // we use the general decorator for Jira, as the userprofile decorator does not work.
        boolean notConfluence = !applicationProperties.getPlatformId().equals("conf");
        context.put("isNotConfluence", notConfluence);

        final String baseUrl = applicationProperties.getBaseUrl(UrlMode.ABSOLUTE);
        context.put("oauthSessionsPage", UriBuilder.fromPath(baseUrl).path("/plugins/servlet/slack/view-oauth-sessions").build());

        eventPublisher.publish(new PageVisitedEvent(analyticsContextProvider.current(), CommonPage.PERSONAL_CONFIG));

        try {
            soyTemplateRenderer.render(
                    response.getWriter(),
                    slackPluginResourceProvider.getPluginKey() + ":slack-personal-settings-resources",
                    "Slack.Templates.Profile.viewPersonalNotificationsPage",
                    context);
        } catch (SoyException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected boolean checkAccess(final HttpServletRequest request) {
        return userManager.getRemoteUser() != null;
    }

    @Override
    protected void onPermissionError(final HttpServletRequest request, final HttpServletResponse response) {
        // do not track this event; it happens when user is unauthenticated, that is fine
    }
}
