package com.atlassian.jira.plugins.slack.model.event;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.plugins.slack.postfunction.SlackPostFunction;

/**
 * This interface represents a workflow transitioning event. This is raised by {@link
 * SlackPostFunction} and has all the information about the
 * transition and the notification to be sent.
 */
public interface JiraPostFunctionEvent extends PluginEvent {
    Issue getIssue();

    ApplicationUser getActor();

    String getFirstStepName();

    String getEndStepName();

    String getActionName();

    String getCustomMessageFormat();

    boolean isHavingErrors();
}
