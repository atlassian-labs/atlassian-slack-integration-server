package com.atlassian.jira.plugins.slack.web.contextproviders;

import com.atlassian.jira.help.HelpUrl;
import com.atlassian.jira.help.HelpUrls;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.customfields.OperationContextImpl;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.OrderableField;
import com.atlassian.jira.issue.operation.IssueOperations;
import com.atlassian.jira.plugins.slack.manager.ProjectConfigurationManager;
import com.atlassian.jira.plugins.slack.model.dto.ProjectToChannelConfigurationDTO;
import com.atlassian.jira.plugins.slack.web.actions.SlackConfigureProjectAction;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.user.UserProjectHistoryManager;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.ContextProvider;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.plugins.slack.rest.model.LimitedSlackLinkDto;
import com.atlassian.plugins.slack.spi.SlackRoutesProviderFactory;
import com.atlassian.plugins.slack.user.SlackUserManager;
import com.github.seratch.jslack.api.model.User;
import com.google.common.collect.ImmutableMap;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import webwork.action.ActionContext;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class SlackGlobalAdminDataProvider implements ContextProvider {
    private final ProjectConfigurationManager projectConfigurationManager;
    private final FieldManager fieldManager;
    private final UserProjectHistoryManager userProjectHistoryManagerBridge;
    private final JiraAuthenticationContext jiraAuthenticationContext;
    private final SlackUserManager slackUserManager;
    private final SlackClientProvider slackClientProvider;
    private final HelpUrls helpUrls;
    private final SlackLinkManager slackLinkManager;
    private final SlackRoutesProviderFactory slackRoutesProviderFactory;

    @Override
    public void init(Map<String, String> map) throws PluginParseException {
        //nothing
    }

    @Override
    public Map<String, Object> getContextMap(final Map<String, Object> map) {
        final Collection<ProjectToChannelConfigurationDTO> configurations =
                projectConfigurationManager.getConfigurations(-1, -1);

        final Collection<LimitedSlackLinkDto> links = slackLinkManager.getLinks()
                .stream()
                .map(LimitedSlackLinkDto::new)
                .collect(Collectors.toList());
        map.put("links", links);
        map.put("projectConfigurations", configurations);
        map.put("projectsField", getProjectsFieldHtml());
        map.put("routes", slackRoutesProviderFactory.getProvider(Collections.emptyMap()));

        final SlackLink link = (SlackLink) map.get("link");
        if (link != null) {
            slackUserManager.getByTeamIdAndUserKey(link.getTeamId(), jiraAuthenticationContext.getLoggedInUser().getKey())
                    .map(slackUser -> {
                        map.put("slackUserId", slackUser.getSlackUserId());
                        return slackClientProvider.withLink(link).getUserInfo(slackUser.getSlackUserId()).fold(
                                e -> slackUser.getSlackUserId(),
                                User::getRealName);
                    })
                    .ifPresent(userName -> map.put("slackUserName", userName));
        }
        map.put(SlackConfigureProjectAction.JQL_HELP_URL_PARAM, getJQLHelpUrl());
        return map;
    }

    private HelpUrl getJQLHelpUrl() {
        return helpUrls.getUrl(SlackConfigureProjectAction.ADVANCED_SEARCH_HELP_KEY);
    }

    private String getProjectsFieldHtml() {
        final OrderableField projectField = ((OrderableField) fieldManager.getField(IssueFieldConstants.PROJECT));
        final Project recentProject = getRecentProject();
        final Map<String, Object> fieldValuesHolder =
                ImmutableMap.of(IssueFieldConstants.PROJECT, recentProject != null ? recentProject.getId() : 0);
        final OperationContextImpl operationContext = new OperationContextImpl(IssueOperations.CREATE_ISSUE_OPERATION,
                fieldValuesHolder);

        Map<String, Object> displayParameters = ImmutableMap.<String, Object>builder()
                .put("theme", "aui")
                .put("noLabel", true)
                .build();

        // We pass new JiraWebActionSupport() so the template has access to i18n
        final JiraWebActionSupport fakeAction = new JiraWebActionSupport();
        ActionContext.getValueStack().pushValue(fakeAction);
        return projectField.getCreateHtml(null, operationContext, fakeAction, null, displayParameters);
    }

    private Project getRecentProject() {
        //noinspection deprecation
        return userProjectHistoryManagerBridge.getCurrentProject(
                Permissions.BROWSE,
                jiraAuthenticationContext.getLoggedInUser());
    }
}
