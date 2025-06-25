package com.atlassian.plugins.slack.oauth2;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugins.slack.admin.AbstractPermissionCheckingServlet;
import com.atlassian.plugins.slack.admin.LoginRedirectionManager;
import com.atlassian.plugins.slack.analytics.AnalyticsContextProvider;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.SlackUser;
import com.atlassian.plugins.slack.api.client.SlackClient;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.atlassian.plugins.slack.api.events.PageVisitedEvent;
import com.atlassian.plugins.slack.api.events.PageVisitedEvent.CommonPage;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.plugins.slack.spi.SlackPluginResourceProvider;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.soy.renderer.SoyException;
import com.atlassian.soy.renderer.SoyTemplateRenderer;
import com.atlassian.webresource.api.assembler.PageBuilderService;
import com.github.seratch.jslack.api.methods.response.auth.AuthTestResponse;
import lombok.Value;
import org.springframework.beans.factory.annotation.Qualifier;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Displays a page showing the user their current oauth sessions and allowing them to remove them.
 */
public class ViewSlackOauthSessionsServlet extends AbstractPermissionCheckingServlet {
    private final SoyTemplateRenderer soyTemplateRenderer;
    private final PageBuilderService pageBuilderService;
    private final SlackClientProvider slackClientProvider;
    private final SlackLinkManager slackLinkManager;
    private final ApplicationProperties applicationProperties;
    private final SlackPluginResourceProvider slackPluginResourceProvider;
    private final EventPublisher eventPublisher;
    private final AnalyticsContextProvider analyticsContextProvider;

    public ViewSlackOauthSessionsServlet(
            @Qualifier("salUserManager") final UserManager userManager,
            final LoginRedirectionManager loginRedirectionManager,
            final SoyTemplateRenderer soyTemplateRenderer,
            final PageBuilderService pageBuilderService,
            final SlackClientProvider slackClientProvider,
            final SlackLinkManager slackLinkManager,
            @Qualifier("salApplicationProperties") final ApplicationProperties applicationProperties,
            final SlackPluginResourceProvider slackPluginResourceProvider,
            final EventPublisher eventPublisher,
            final AnalyticsContextProvider analyticsContextProvider) {
        super(userManager, loginRedirectionManager);
        this.soyTemplateRenderer = soyTemplateRenderer;
        this.pageBuilderService = pageBuilderService;
        this.slackClientProvider = slackClientProvider;
        this.slackLinkManager = slackLinkManager;
        this.applicationProperties = applicationProperties;
        this.slackPluginResourceProvider = slackPluginResourceProvider;
        this.eventPublisher = eventPublisher;
        this.analyticsContextProvider = analyticsContextProvider;
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        pageBuilderService.assembler().resources().requireWebResource(slackPluginResourceProvider.getPluginKey() + ":slack-view-oauth-resources");
        response.setContentType("text/html;charset=UTF-8");
        final Map<String, Object> context = new HashMap<>();

        final List<SessionData> sessions = slackLinkManager.getLinks().stream()
                .map(slackClientProvider::withLink)
                .map(client -> {
                    final SlackLink link = client.getLink();

                    final Optional<SlackClient> userClient = client.withRemoteUserTokenIfAvailable();
                    final Optional<String> slackUserId = userClient
                            .flatMap(SlackClient::getUser)
                            .map(SlackUser::getSlackUserId);
                    @SuppressWarnings("Convert2MethodRef") final Optional<String> slackUserName = slackUserId
                            .flatMap(userId -> client.getUserInfo(userId).toOptional())
                            // do not inline user.getRealName(), it fails to compile on JDK 11
                            .map(user -> user.getRealName());
                    final Optional<String> tokenStatus = userClient
                            .map(uc -> uc.testToken().fold(
                                    e -> e.getApiResponse(AuthTestResponse.class).fold(
                                            ex -> "NO_CONNECTION",
                                            AuthTestResponse::getError
                                    ),
                                    r -> "CONNECTED"));

                    return new SessionData(
                            link.getTeamId(),
                            link.getTeamName(),
                            slackUserName.orElse("-"),
                            slackUserId.orElse(""),
                            tokenStatus.orElse(""));
                })
                .collect(Collectors.toList());
        context.put("sessions", sessions);

        // we use the general decorator for Jira, as the userprofile decorator does not work.
        boolean notConfluence = !applicationProperties.getPlatformId().equals("conf");
        context.put("isNotConfluence", notConfluence);
        context.put("decorator", notConfluence ? "atl.general" : "atl.userprofile");

        eventPublisher.publish(new PageVisitedEvent(analyticsContextProvider.current(), CommonPage.OAUTH_SESSION));

        try {
            soyTemplateRenderer.render(
                    response.getWriter(),
                    slackPluginResourceProvider.getPluginKey() + ":slack-view-oauth-resources",
                    "Slack.Templates.Profile.viewOauthSessionsPage",
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

    @Value
    public static final class SessionData {
        final String teamId;
        final String teamName;
        final String slackUserName;
        final String slackUserId;
        final String tokenStatus;
    }
}
