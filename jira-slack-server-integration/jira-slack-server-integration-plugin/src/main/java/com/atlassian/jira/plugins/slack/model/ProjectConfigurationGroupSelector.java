package com.atlassian.jira.plugins.slack.model;

/**
 * This interface is the data type by which we can look up a single project configuration group. Ideally, only providing
 * project configuration group ID should be enough, but providing project ID can provide more assurance.
 */
public interface ProjectConfigurationGroupSelector {
    long getProjectId();

    String getProjectConfigurationGroupId();
}
