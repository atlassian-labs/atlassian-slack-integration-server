package com.atlassian.jira.plugins.slack.service.issuefilter;

import com.atlassian.jira.plugins.slack.model.EventFilterType;
import com.atlassian.jira.plugins.slack.model.event.JiraIssueEvent;

import jakarta.validation.constraints.NotNull;

/**
 * Issue filter that is related 1-1 with an EventFilterType
 */
public interface IssueFilter {
    /**
     * We validate that the filter applies to this issue
     *
     * We pass the event cause maybe in the future we need to get information
     * on how it was generated
     * @param event the issue event
     * @param value the value to match
     * @return true if it applies a filter , false if not
     */
    boolean apply(final JiraIssueEvent event, @NotNull final String value);

    /**
     * Returns the map of the filter type
     * @return the EventFilterType that is related
     */
    EventFilterType getEventFilterType();
}
