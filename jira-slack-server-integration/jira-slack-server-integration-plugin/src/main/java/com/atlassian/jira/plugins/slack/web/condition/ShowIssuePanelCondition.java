package com.atlassian.jira.plugins.slack.web.condition;

import com.atlassian.jira.plugins.slack.manager.PluginConfigurationManager;
import com.atlassian.jira.plugins.slack.manager.ProjectConfigurationManager;
import com.atlassian.jira.project.Project;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;

import java.util.Map;

public class ShowIssuePanelCondition implements Condition {
    private final PluginConfigurationManager pluginConfigurationManager;
    private final ProjectConfigurationManager projectConfigurationManager;

    public ShowIssuePanelCondition(final PluginConfigurationManager pluginConfigurationManager,
                                   final ProjectConfigurationManager projectConfigurationManager) {
        this.pluginConfigurationManager = pluginConfigurationManager;
        this.projectConfigurationManager = projectConfigurationManager;
    }

    @Override
    public void init(final Map<String, String> params) throws PluginParseException {
    }

    @Override
    public boolean shouldDisplay(final Map<String, Object> context) {
        final Project project = (Project) context.get("project");
        final boolean isPanelDisabledForProject = project != null
                && projectConfigurationManager.isIssuePanelHidden(project);
        return !isPanelDisabledForProject && !pluginConfigurationManager.isIssuePanelHidden();
    }
}
