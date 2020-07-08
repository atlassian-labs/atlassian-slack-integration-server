package com.atlassian.plugins.slack.api;

public interface SlackUser {
    String getSlackUserId();

    String getUserKey();

    String getSlackTeamId();

    String getUserToken();

    String getConnectionError();
}
