package com.atlassian.jira.plugins.slack.web.actions;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.help.HelpUrl;
import com.atlassian.jira.help.HelpUrls;
import com.atlassian.jira.plugins.slack.manager.ProjectConfigurationManager;
import com.atlassian.jira.plugins.slack.model.dto.ProjectToChannelConfigurationDTO;
import com.atlassian.jira.plugins.slack.model.event.JiraPage;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.projectconfig.util.ProjectConfigRequestCache;
import com.atlassian.jira.security.request.RequestMethod;
import com.atlassian.jira.security.request.SupportedMethods;
import com.atlassian.jira.util.velocity.VelocityRequestContextFactory;
import com.atlassian.jira.web.action.ActionViewDataMappings;
import com.atlassian.plugins.slack.analytics.AnalyticsContextProvider;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.atlassian.plugins.slack.api.events.PageVisitedEvent;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.plugins.slack.spi.SlackRoutesProviderFactory;
import com.atlassian.plugins.slack.user.SlackUserManager;
import com.atlassian.webresource.api.assembler.PageBuilderService;
import com.google.common.collect.ImmutableMap;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * Be careful when naming actions: https://jira.atlassian.com/browse/JRASERVER-26275
 */
@SupportedMethods(RequestMethod.GET)
public class SlackConfigureProjectAction extends AbstractProjectAction {
    public static final String JQL_HELP_URL_PARAM = "jqlHelpUrl";
    public static final String ADVANCED_SEARCH_HELP_KEY = "advanced_search";

    private final HelpUrls helpUrls;
    private final EventPublisher eventPublisher;
    private final AnalyticsContextProvider analyticsContextProvider;

    public SlackConfigureProjectAction(final VelocityRequestContextFactory contextFactory,
                                       final PageBuilderService pageBuilderService,
                                       final ProjectConfigRequestCache projectConfigRequestCache,
                                       final ProjectConfigurationManager projectConfigurationManager,
                                       final SlackUserManager slackUserManager,
                                       final SlackRoutesProviderFactory slackRoutesProviderFactory,
                                       final HelpUrls helpUrls,
                                       final SlackClientProvider slackClientProvider,
                                       final SlackLinkManager slackLinkManager,
                                       final EventPublisher eventPublisher,
                                       final AnalyticsContextProvider analyticsContextProvider) {
        super(contextFactory, pageBuilderService, projectConfigRequestCache, projectConfigurationManager,
                slackUserManager, slackRoutesProviderFactory, slackClientProvider, slackLinkManager);
        this.helpUrls = helpUrls;
        this.eventPublisher = eventPublisher;
        this.analyticsContextProvider = analyticsContextProvider;
    }

    @Override
    protected String doExecute() throws Exception {
        String superResult = super.doExecute();
        if (!SUCCESS.equals(superResult)) {
            return superResult;
        }

        eventPublisher.publish(new PageVisitedEvent(analyticsContextProvider.current(), JiraPage.PROJECT_CONFIG));

        return SUCCESS;
    }

    @ActionViewDataMappings({SUCCESS})
    public Map<String, Object> getDataMap() {
        final Project project = getProject();
        if (project == null) {
            return Collections.emptyMap();
        }
        final ProjectToChannelConfigurationDTO projectConfiguration = projectConfigurationManager.getConfiguration(project.getId());
        final HelpUrl jqlHelpUrl = helpUrls.getUrl(ADVANCED_SEARCH_HELP_KEY);
        final Optional<SlackLink> link = getLink();
        ImmutableMap.Builder<String, Object> builder = ImmutableMap.<String, Object>builder()
                .put("links", getLinks())
                .put("projectKey", getProjectKey())
                .put("projectConfiguration", projectConfiguration)
                .put("projectId", project.getId())
                .put("routes", getSlackRoutesProvider())
                .put("projectName", project.getName())
                .put("slackUserName", link.flatMap(this::getSlackUserName).orElse(""))
                .put("slackUserId", link.flatMap(this::getSlackUserId).orElse(""))
                .put("jqlHelpUrl", jqlHelpUrl);
        link.ifPresent(l -> builder.put("link", l));
        return builder.build();
    }

    protected String getRequestUrl(HttpServletRequest request) {
        return "/secure/ConfigureSlack.jspa?" + request.getQueryString();
    }
}
