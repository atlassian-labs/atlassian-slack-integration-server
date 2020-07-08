package com.atlassian.jira.plugins.slack.service.issuefilter.impl;

import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.plugins.slack.model.EventFilterType;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;

/**
 * Returns the issue priority filter
 */
@Service
public class IssuePriorityFilter extends AbstractSimpleIssueFilter {
    @Override
    public String getIssueValue(@NotNull final IssueEvent event) {
        if (event.getIssue() == null || event.getIssue().getPriority() == null) {
            return null;
        }
        return event.getIssue().getPriority().getId();
    }

    @Override
    public EventFilterType getEventFilterType() {
        return EventFilterType.ISSUE_PRIORITY;
    }
}
