package com.atlassian.jira.plugins.slack.model.event;

import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.plugins.slack.model.EventMatcherType;

/**
 * This interface contains information about an issue event sent from Jira.
 */
public interface JiraIssueEvent extends PluginEvent {
    /**
     * Returns the issue event from jira
     *
     * @return the issue event
     */
    IssueEvent getIssueEvent();

    EventMatcherType getEventMatcher();
}
