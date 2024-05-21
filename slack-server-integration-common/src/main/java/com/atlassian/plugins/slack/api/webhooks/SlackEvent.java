package com.atlassian.plugins.slack.api.webhooks;

import com.atlassian.plugins.slack.api.SlackLink;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class SlackEvent {
    private String teamId;
    private String eventId;
    private int eventTime;
    @JsonIgnore
    private SlackLink slackLink;

    public SlackEvent(final String teamId,
                      final String eventId,
                      final int eventTime,
                      final SlackLink slackLink) {
        this.teamId = teamId;
        this.eventId = eventId;
        this.eventTime = eventTime;
        this.slackLink = slackLink;
    }

    public String getTeamId() {
        return teamId;
    }

    public String getEventId() {
        return eventId;
    }

    public int getEventTime() {
        return eventTime;
    }

    public SlackLink getSlackLink() {
        return slackLink;
    }
}
