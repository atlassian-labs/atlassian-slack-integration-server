package com.atlassian.plugins.slack.api.webhooks.action;

import org.codehaus.jackson.annotate.JsonProperty;

import java.util.Map;

public class SlackAction {
    protected String type;
    protected String teamId;
    protected String channelId;
    protected String userId;

    public SlackAction(final String type) {
        this.type = type;
    }

    @JsonProperty("team")
    public void unpackTeam(final Map<String, String> team) {
        teamId = team.get("id");
    }

    @JsonProperty("channel")
    public void unpackChannel(final Map<String, String> channel) {
        channelId = channel.get("id");
    }

    @JsonProperty("user")
    public void unpackUser(final Map<String, String> user) {
        userId = user.get("id");
    }

    public String getType() {
        return type;
    }

    public String getTeamId() {
        return teamId;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getUserId() {
        return userId;
    }
}
