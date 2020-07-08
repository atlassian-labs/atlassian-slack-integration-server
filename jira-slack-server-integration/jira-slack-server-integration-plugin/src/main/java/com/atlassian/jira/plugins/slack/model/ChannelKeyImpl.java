package com.atlassian.jira.plugins.slack.model;

import java.util.Objects;

public class ChannelKeyImpl implements ChannelKey {
    private final String userId;
    private final String teamId;
    private final String channelId;

    /**
     * @param userId An empty userId will fall back to bot ID
     */
    public ChannelKeyImpl(final String userId,
                          final String teamId,
                          final String channelId) {
        this.userId = userId;
        this.teamId = teamId;
        this.channelId = channelId;
    }

    public String getUserId() {
        return userId;
    }

    @Override
    public String getTeamId() {
        return teamId;
    }

    @Override
    public String getChannelId() {
        return channelId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof ChannelKeyImpl)) return false;
        final ChannelKeyImpl that = (ChannelKeyImpl) o;
        return Objects.equals(userId, that.userId) &&
                Objects.equals(teamId, that.teamId) &&
                Objects.equals(channelId, that.channelId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, teamId, channelId);
    }
}
