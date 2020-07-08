package com.atlassian.jira.plugins.slack.util.matcher;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugins.slack.model.ProjectConfiguration;
import com.atlassian.jira.plugins.slack.model.dto.MultipleValue;

/**
 * This workflow event matcher will check if the event
 * can be tracked
 */
public class WorkflowStatusMatcher implements EventMatcher {
    @Override
    public boolean matches(Issue issue, ProjectConfiguration config) {
        final String id = issue.getStatus().getId();
        return new MultipleValue(config.getValue()).apply(id);
    }
}
