package com.atlassian.jira.plugins.slack.model.dto;

import com.atlassian.jira.plugins.slack.model.DedicatedChannel;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = ANY)
public class DedicatedChannelDTO implements DedicatedChannel {
    private long issueId;
    private String name;
    private String teamId;
    private String channelId;
    private boolean privateChannel;
    private String creator;

    @JsonCreator
    public DedicatedChannelDTO(@JsonProperty("issueId") final long issueId,
                               @JsonProperty("name") final String name,
                               @JsonProperty("teamId") final String teamId,
                               @JsonProperty("channelId") final String channelId,
                               @JsonProperty("privateChannel") final boolean privateChannel,
                               @JsonProperty("creator") final String creator) {
        this.issueId = issueId;
        this.name = name;
        this.teamId = teamId;
        this.channelId = channelId;
        this.privateChannel = privateChannel;
        this.creator = creator;
    }

    @Override
    public long getIssueId() {
        return issueId;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getChannelId() {
        return channelId;
    }

    @Override
    public String getKey() {
        return String.valueOf(issueId);
    }

    @Override
    public String getTeamId() {
        return teamId;
    }

    @Override
    public boolean isPrivateChannel() {
        return privateChannel;
    }

    @Override
    public String getCreator() {
        return creator;
    }

    @Override
    public String toString() {
        return "DedicatedChannelDTO{" +
                "issueId=" + issueId +
                ", name='" + name + '\'' +
                ", channelId=" + channelId +
                ", teamId=" + teamId +
                ", privateChannel=" + privateChannel +
                ", creator=" + creator +
                '}';
    }
}
