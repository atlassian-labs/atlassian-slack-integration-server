package com.atlassian.plugins.slack.rest.model;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.Objects;

import static org.codehaus.jackson.annotate.JsonAutoDetect.Visibility.ANY;
import static org.codehaus.jackson.annotate.JsonAutoDetect.Visibility.NONE;

/**
 * This class contains Slack channel information
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = ANY, isGetterVisibility = NONE, getterVisibility = NONE)
public class SlackChannelDTO {
    private final String teamId;
    private final String teamName;
    private final String channelId;
    private final String channelName;
    @JsonProperty("isPrivate")
    private final boolean isPrivate;

    public SlackChannelDTO(final String teamId,
                           final String teamName,
                           final String channelId,
                           final String channelName,
                           final boolean isPrivate) {
        this.teamId = teamId;
        this.teamName = teamName;
        this.channelName = channelName;
        this.channelId = channelId;
        this.isPrivate = isPrivate;
    }

    public String getTeamId() {
        return teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getChannelName() {
        return channelName;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof SlackChannelDTO)) return false;
        final SlackChannelDTO that = (SlackChannelDTO) o;
        return teamId.equals(that.teamId) &&
                channelId.equals(that.channelId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(teamId, channelId);
    }
}
