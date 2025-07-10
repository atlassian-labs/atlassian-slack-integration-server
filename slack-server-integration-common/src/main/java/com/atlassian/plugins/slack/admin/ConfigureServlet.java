package com.atlassian.plugins.slack.admin;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugin.web.WebInterfaceManager;
import com.atlassian.plugins.slack.analytics.AnalyticsContextProvider;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.events.PageVisitedEvent;
import com.atlassian.plugins.slack.api.events.PageVisitedEvent.CommonPage;
import com.atlassian.plugins.slack.api.events.SlackRegistrationPageHitByNonAdminUser;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.plugins.slack.spi.ConfigurationRedirectionManager;
import com.atlassian.plugins.slack.spi.SlackLinkAccessManager;
import com.atlassian.plugins.slack.spi.SlackPluginResourceProvider;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.UrlMode;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.google.common.collect.ImmutableMap;
import io.atlassian.fugue.Either;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Qualifier;

import java.io.IOException;
import java.net.URI;
import java.util.Enumeration;
import java.util.Map;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

public class ConfigureServlet extends AbstractAdminServlet {
    private static final String CONTENT_TYPE = "text/html;charset=UTF-8";
    private static final String TEMPLATE = "admin/configure-slack.vm";
    private static final String TEMPLATE_EDIT = "admin/connect/connect-workspace.vm";
    private static final String CONTEXT_ATTRIBUTE_LABEL = "context";
    public static final String SLACK_EPHEMERAL_ATTRIBUTE_PREFIX = "slack.integration.attribute.";

    private final TemplateRenderer templateRenderer;
    private final ConfigurationRedirectionManager configurationRedirectionManager;
    private final WebInterfaceManager webInterfaceManager;
    private final SlackLinkManager slackLinkManager;
    private final EventPublisher eventPublisher;
    private final ApplicationProperties applicationProperties;
    private final SlackPluginResourceProvider slackPluginResourceProvider;
    private final AnalyticsContextProvider analyticsContextProvider;

    public ConfigureServlet(final TemplateRenderer templateRenderer,
                            final SlackLinkAccessManager slackLinkAccessManager,
                            @Qualifier("salUserManager") final UserManager userManager,
                            final LoginRedirectionManager loginRedirectionManager,
                            @Qualifier("defaultConfigurationRedirectionManager") final ConfigurationRedirectionManager configurationRedirectionManager,
                            final WebInterfaceManager webInterfaceManager,
                            final SlackLinkManager slackLinkManager,
                            final EventPublisher eventPublisher,
                            @Qualifier("salApplicationProperties") final ApplicationProperties applicationProperties,
                            final SlackPluginResourceProvider slackPluginResourceProvider,
                            final AnalyticsContextProvider analyticsContextProvider) {
        super(userManager, loginRedirectionManager, slackLinkAccessManager);
        this.templateRenderer = templateRenderer;
        this.configurationRedirectionManager = configurationRedirectionManager;
        this.webInterfaceManager = webInterfaceManager;
        this.slackLinkManager = slackLinkManager;
        this.eventPublisher = eventPublisher;
        this.applicationProperties = applicationProperties;
        this.slackPluginResourceProvider = slackPluginResourceProvider;
        this.analyticsContextProvider = analyticsContextProvider;
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        final String action = request.getParameter("action");
        final String recentInstall = request.getParameter("recentInstall");
        final String teamId = defaultString(recentInstall, request.getParameter("teamId"));

        HttpSession session = request.getSession();
        Optional<URI> configurationRedirectUri = getRedirectUri(request);

        if (!configurationRedirectUri.isPresent()) {
            boolean isConfluence = ApplicationProperties.PLATFORM_CONFLUENCE.equals(applicationProperties.getPlatformId());
            if ("add".equals(action)) {
                Map<String, Object> context = ImmutableMap.of(
                        "baseUrl", applicationProperties.getBaseUrl(UrlMode.ABSOLUTE),
                        "slackPluginResourceProvider", slackPluginResourceProvider,
                        "webInterfaceManager", webInterfaceManager,
                        "isConfluence", isConfluence);
                eventPublisher.publish(new PageVisitedEvent(analyticsContextProvider.current(), CommonPage.CONNECT_TEAM));
                render(TEMPLATE_EDIT, context, response);
            } else if ("edit".equals(action) && isNotEmpty(teamId)) {
                final Either<Throwable, SlackLink> linkById = slackLinkManager.getLinkByTeamId(teamId);
                if (linkById.isRight()) {
                    Map<String, Object> context = ImmutableMap.of(
                            "webInterfaceManager", webInterfaceManager,
                            "baseUrl", applicationProperties.getBaseUrl(UrlMode.ABSOLUTE),
                            "slackPluginResourceProvider", slackPluginResourceProvider,
                            "link", linkById.getOrNull(),
                            "isConfluence", isConfluence);
                    eventPublisher.publish(new PageVisitedEvent(analyticsContextProvider.current(), CommonPage.EDIT_TEAM));
                    render(TEMPLATE_EDIT, context, response);
                } else {
                    response.sendError(404);
                }
            } else {
                if (isNotEmpty(teamId)) {
                    final Either<Throwable, SlackLink> linkById = slackLinkManager.getLinkByTeamId(teamId);
                    if (linkById.isRight()) {
                        Map<String, Object> context = ImmutableMap.of(
                                "link", linkById.getOrNull(),
                                "webInterfaceManager", webInterfaceManager,
                                "slackPluginResourceProvider", slackPluginResourceProvider,
                                "recentInstall", defaultString(recentInstall, ""));
                        eventPublisher.publish(new PageVisitedEvent(analyticsContextProvider.byTeamId(teamId),
                                CommonPage.GLOBAL_CONFIG));
                        render(TEMPLATE, context, response);
                    } else {
                        response.sendError(404);
                    }
                } else {
                    ImmutableMap.Builder<String, Object> context = ImmutableMap.builder();
                    Optional<SlackLink> slackLink = slackLinkManager.getLinks().stream().findFirst();
                    slackLink.ifPresent(link -> context.put("link", link));
                    eventPublisher.publish(new PageVisitedEvent(analyticsContextProvider.bySlackLink(
                            slackLink.orElse(null)), CommonPage.GLOBAL_CONFIG));
                    context.put("webInterfaceManager", webInterfaceManager);
                    context.put("recentInstall", defaultString(recentInstall, ""));
                    context.put("slackPluginResourceProvider", slackPluginResourceProvider);
                    render(TEMPLATE, context.build(), response);
                }
            }
        } else {
            Map<String, Object> context = ImmutableMap.of(
                    "webInterfaceManager", webInterfaceManager,
                    "recentInstall", defaultString(recentInstall, ""));
            session.setAttribute(CONTEXT_ATTRIBUTE_LABEL, context);
            response.sendRedirect(configurationRedirectUri.get().toString());
        }
        removeEphemeralAttributesFromSession(session);
    }

    @Override
    protected void onPermissionError(final HttpServletRequest request, final HttpServletResponse response) {
        // We want to know when the install page was shown in response to a particular bit of advertising
        String src = request.getParameter("src");
        if (isNotBlank(src)) {
            eventPublisher.publish(new SlackRegistrationPageHitByNonAdminUser(analyticsContextProvider.current(), src));
        }
    }

    private void removeEphemeralAttributesFromSession(HttpSession session) {
        final Enumeration attributeNames = session.getAttributeNames();
        while (attributeNames.hasMoreElements()) {
            final String attributeName = (String) attributeNames.nextElement();
            if (attributeName.startsWith(SLACK_EPHEMERAL_ATTRIBUTE_PREFIX)) {
                session.removeAttribute(attributeName);
            }
        }
    }

    private Optional<URI> getRedirectUri(HttpServletRequest request) {
        return configurationRedirectionManager.getRedirectUri(request);
    }

    private void render(String template, Map<String, Object> context, HttpServletResponse resp)
            throws ServletException {
        resp.setContentType(CONTENT_TYPE);
        try {
            templateRenderer.render(template, context, resp.getWriter());
        } catch (IOException e) {
            throw new ServletException(e);
        }
    }
}
