package com.atlassian.jira.plugins.slack.service.issuefilter.impl;

import com.atlassian.jira.plugins.slack.model.ProjectConfiguration;
import com.atlassian.jira.plugins.slack.service.issuefilter.IssueFilter;

import static com.google.common.base.Preconditions.checkNotNull;

public class IssueFilterConfiguration {
    private final IssueFilter filter;
    private final ProjectConfiguration configuration;

    IssueFilterConfiguration(IssueFilter filter, ProjectConfiguration configuration) {
        this.filter = checkNotNull(filter);
        this.configuration = checkNotNull(configuration);
    }

    public IssueFilter getFilter() {
        return filter;
    }

    public ProjectConfiguration getConfiguration() {
        return configuration;
    }
}
