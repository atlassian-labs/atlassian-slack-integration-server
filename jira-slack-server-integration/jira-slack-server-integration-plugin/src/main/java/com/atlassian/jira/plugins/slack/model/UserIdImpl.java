package com.atlassian.jira.plugins.slack.model;

import java.util.Objects;

public class UserIdImpl implements UserId {
    private String teamId;
    private String userId;

    public UserIdImpl(String teamId, String userId) {
        this.teamId = teamId;
        this.userId = userId;
    }

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(final String teamId) {
        this.teamId = teamId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(final String userId) {
        this.userId = userId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof UserIdImpl)) return false;
        final UserIdImpl that = (UserIdImpl) o;
        return Objects.equals(teamId, that.teamId) &&
                Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(teamId, userId);
    }
}
