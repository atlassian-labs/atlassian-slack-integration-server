package com.atlassian.jira.plugins.slack.service.issuefilter.impl;

import com.atlassian.jira.plugins.slack.model.EventFilterType;
import com.atlassian.jira.plugins.slack.model.event.JiraIssueEvent;
import org.springframework.stereotype.Service;

import jakarta.validation.constraints.NotNull;

/**
 * Issue type filters can be grouped and separated by comma If null, or "ALL" applies then everything can pass through
 * this filter
 */
@Service
public class IssueTypeFilter extends AbstractSimpleIssueFilter {
    @Override
    public EventFilterType getEventFilterType() {
        return EventFilterType.ISSUE_TYPE;
    }

    @Override
    public String getIssueValue(@NotNull final JiraIssueEvent event) {
        return event.getIssue().getIssueTypeId();
    }

}
