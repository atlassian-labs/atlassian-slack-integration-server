package com.atlassian.jira.plugins.slack.web.actions;

import com.atlassian.jira.plugins.slack.manager.PluginConfigurationManager;
import com.atlassian.jira.plugins.slack.manager.ProjectConfigurationManager;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.web.action.ActionViewDataMappings;
import com.atlassian.webresource.api.assembler.PageBuilderService;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import webwork.action.ServletActionContext;

import java.util.Map;

/**
 * We edit the project settings from here There is a shared scope between EditProjectSettings and EditGlobalSettings so
 * it's better to inherit for later use
 */
public class SlackEditProjectSettings extends SlackEditGlobalSettings {
    private static final String PROJECT_KEY = "projectKey";
    private static final String ALLOW_PROJECT_AUTOCONVERT = "allowProjectAutoConvert";
    private static final String HIDE_ISSUE_PANEL_PROJECT = "hideIssuePanelProject";
    private static final String SEND_RESTRICTED_COMMENTS_TO_DEDICATED = "sendRestrictedCommentsToDedicated";

    private final ProjectManager projectManager;
    private final ProjectConfigurationManager projectConfigurationManager;

    @Autowired
    public SlackEditProjectSettings(final PluginConfigurationManager pluginConfigurationManager,
                                    final ProjectManager projectManager,
                                    final ProjectConfigurationManager projectConfigurationManager,
                                    final PageBuilderService pageBuilderService) {
        super(pluginConfigurationManager, pageBuilderService);
        this.projectManager = projectManager;
        this.projectConfigurationManager = projectConfigurationManager;
    }

    @ActionViewDataMappings({SUCCESS})
    public Map<String, Object> getDataMap() {
        final String projectKey = getProjectKey();
        final Project project = projectManager.getProjectByCurrentKey(projectKey);

        return ImmutableMap.<String, Object>builder()
                .put(PROJECT_KEY, projectKey)
                .put(ALLOW_PROJECT_AUTOCONVERT,
                        projectConfigurationManager.isProjectAutoConvertEnabled(project))
                .put(ALLOW_GLOBAL_AUTOCONVERT, pluginConfigurationManager.isGlobalAutoConvertEnabled())
                .put(HIDE_ISSUE_PANEL, pluginConfigurationManager.isIssuePanelHidden())
                .put(HIDE_ISSUE_PANEL_PROJECT, projectConfigurationManager.isIssuePanelHidden(project))
                .put(SEND_RESTRICTED_COMMENTS_TO_DEDICATED,
                        projectConfigurationManager.shouldSendRestrictedCommentsToDedicatedChannels(project))
                .build();
    }

    private String getProjectKey() {
        final String projectKey = ServletActionContext.getRequest().getParameter(PROJECT_KEY);
        if (StringUtils.isBlank(projectKey)) {
            throw new RuntimeException("project was not set");
        }
        return projectKey;
    }
}
