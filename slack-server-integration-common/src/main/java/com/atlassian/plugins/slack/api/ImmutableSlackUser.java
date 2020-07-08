package com.atlassian.plugins.slack.api;

import java.io.Serializable;

public final class ImmutableSlackUser implements SlackUser, Serializable {
    private final String slackUserId;
    private final String userKey;
    private final String slackTeamId;
    private final String userToken;
    private final String connectionError;

    public ImmutableSlackUser(final SlackUser user) {
        this(user.getSlackUserId(), user.getUserKey(), user.getSlackTeamId(), user.getUserToken(), user.getConnectionError());
    }

    public ImmutableSlackUser(final String slackUserId,
                       final String userKey,
                       final String slackTeamId,
                       final String userToken,
                       final String connectionError) {
        this.slackUserId = slackUserId;
        this.userKey = userKey;
        this.slackTeamId = slackTeamId;
        this.userToken = userToken;
        this.connectionError = connectionError;
    }

    public String getSlackUserId() {
        return slackUserId;
    }

    public String getUserKey() {
        return userKey;
    }

    public String getSlackTeamId() {
        return slackTeamId;
    }

    public String getUserToken() {
        return userToken;
    }

    @Override
    public String getConnectionError() {
        return connectionError;
    }
}
