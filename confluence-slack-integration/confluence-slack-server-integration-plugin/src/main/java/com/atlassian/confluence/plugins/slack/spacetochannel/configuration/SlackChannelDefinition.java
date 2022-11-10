package com.atlassian.confluence.plugins.slack.spacetochannel.configuration;

import com.google.common.base.Preconditions;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.Comparator;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SlackChannelDefinition {
    private final String teamName;
    private final String teamId;
    private final String channelName;
    private final String channelId;
    private final boolean isPrivate;
    private final boolean muted;

    @JsonCreator
    public SlackChannelDefinition(
            @JsonProperty("teamName") String teamName,
            @JsonProperty("teamId") String teamId,
            @JsonProperty("channelName") String channelName,
            @JsonProperty("channelId") String channelId,
            @JsonProperty("isPrivate") boolean isPrivate) {
        this(teamName, teamId, channelName, channelId, isPrivate, false);
    }

    public SlackChannelDefinition(final String teamName,
                                  final String teamId,
                                  final String channelName,
                                  final String channelId,
                                  final boolean isPrivate,
                                  final boolean muted) {
        this.teamName = teamName;
        this.teamId = teamId;
        this.channelName = Preconditions.checkNotNull(channelName);
        this.channelId = Preconditions.checkNotNull(channelId);
        this.isPrivate = isPrivate;
        this.muted = muted;
    }

    @JsonProperty
    public String getTeamName() {
        return teamName;
    }

    @JsonProperty
    public String getTeamId() {
        return teamId;
    }

    @JsonProperty
    public String getChannelName() {
        return channelName;
    }

    @JsonProperty
    public String getChannelId() {
        return channelId;
    }

    @JsonProperty("isPrivate")
    public boolean isPrivate() {
        return isPrivate;
    }

    public boolean isMuted() {
        return muted;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SlackChannelDefinition that = (SlackChannelDefinition) o;

        return Objects.equals(channelId, that.channelId);
    }

    @Override
    public int hashCode() {
        return channelId != null ? channelId.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "SlackChannelDefinition{" +
                "channelId='" + channelId + '\'' +
                ", channelName='" + channelName + '\'' +
                ", isPrivate='" + isPrivate + '\'' +
                '}';
    }

    public static final Comparator<SlackChannelDefinition> ORDER_BY_NAME = (channel1, channel2) ->
            String.CASE_INSENSITIVE_ORDER.compare(channel1.getChannelName(), channel2.getChannelName());
}
