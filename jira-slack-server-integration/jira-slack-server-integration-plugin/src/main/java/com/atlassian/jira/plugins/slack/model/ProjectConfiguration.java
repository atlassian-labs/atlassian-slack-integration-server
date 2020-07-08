package com.atlassian.jira.plugins.slack.model;

public interface ProjectConfiguration extends BaseObject {
    String getTeamId();

    String getChannelId();

    long getProjectId();

    String getConfigurationGroupId();

    String getName();

    String getValue();
}
