package com.atlassian.jira.plugins.slack.model;

import com.atlassian.plugins.slack.api.SlackLink;

public class SlackDeletedMessage {
    private String teamId;
    private SlackLink slackLink;
    private final String channelId;
    private final String ts;

    public SlackDeletedMessage(
            final String teamId,
            final SlackLink slackLink,
            final String channelId,
            final String ts) {
        this.teamId = teamId;
        this.slackLink = slackLink;
        this.channelId = channelId;
        this.ts = ts;
    }

    public String getTeamId() {
        return teamId;
    }

    public SlackLink getSlackLink() {
        return slackLink;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getTs() {
        return ts;
    }
}
