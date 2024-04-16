package com.atlassian.plugins.slack.rest.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = ANY)
public class SlackWebHookPayload {
    @JsonProperty("token")
    private String verificationToken;
    @JsonProperty("team_id")
    private String teamId;
    @JsonProperty
    private JsonNode event;
    @JsonProperty("event_id")
    private String eventId;
    @JsonProperty("event_time")
    private int eventTime;

    @JsonCreator
    public SlackWebHookPayload(@JsonProperty("token") final String verificationToken,
                               @JsonProperty("team_id") final String teamId,
                               @JsonProperty("event") final JsonNode event,
                               @JsonProperty("event_id") final String eventId,
                               @JsonProperty("event_time") final int eventTime) {
        this.verificationToken = verificationToken;
        this.teamId = teamId;
        this.event = event;
        this.eventId = eventId;
        this.eventTime = eventTime;
    }

    @SuppressWarnings("unused")
    public String getVerificationToken() {
        return verificationToken;
    }

    public String getTeamId() {
        return teamId;
    }

    public JsonNode getEvent() {
        return event;
    }

    public String getEventId() {
        return eventId;
    }

    public int getEventTime() {
        return eventTime;
    }

    public String getType() {
        return event.path("type").asText();
    }
}
