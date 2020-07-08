package com.atlassian.jira.plugins.slack.service.issuefilter.impl;

import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.plugins.slack.model.dto.MultipleValue;
import com.atlassian.jira.plugins.slack.service.issuefilter.IssueFilter;

import javax.validation.constraints.NotNull;

/**
 * This simple issue filter does everything with simple strings and multiple value string
 */
public abstract class AbstractSimpleIssueFilter implements IssueFilter {
    @Override
    public boolean apply(IssueEvent event, @NotNull String value) {
        final String issueValue = getIssueValue(event);
        return new MultipleValue(value).apply(issueValue);
    }


    public abstract String getIssueValue(@NotNull final IssueEvent event);
}
