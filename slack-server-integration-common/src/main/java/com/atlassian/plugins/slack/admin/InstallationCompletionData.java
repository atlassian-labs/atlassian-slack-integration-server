package com.atlassian.plugins.slack.admin;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class InstallationCompletionData {
    @JsonProperty
    private final String teamName;
    @JsonProperty
    private final String teamId;

    public InstallationCompletionData(final String teamName, final String teamId) {
        this.teamName = teamName;
        this.teamId = teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public String getTeamId() {
        return teamId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final InstallationCompletionData that = (InstallationCompletionData) o;
        return Objects.equals(teamName, that.teamName) &&
                Objects.equals(teamId, that.teamId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(teamName, teamId);
    }
}
