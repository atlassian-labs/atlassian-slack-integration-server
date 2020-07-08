package com.atlassian.jira.plugins.slack.model;

public interface ChannelKey {
    String getUserId();

    String getTeamId();

    String getChannelId();
}
