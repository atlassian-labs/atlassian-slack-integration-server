package com.atlassian.jira.plugins.slack.model;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import static org.codehaus.jackson.annotate.JsonAutoDetect.Visibility.ANY;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = ANY)
public class DedicatedChannelInfo {
    private final String issueKey;
    private final String channelId;
    private final String teamId;

    public DedicatedChannelInfo(@JsonProperty("issueKey") final String issueKey,
                                @JsonProperty("channelId") final String channelId,
                                @JsonProperty("teamId") final String teamId) {
        this.issueKey = issueKey;
        this.channelId = channelId;
        this.teamId = teamId;
    }

    public String getIssueKey() {
        return issueKey;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getTeamId() {
        return teamId;
    }

    @Override
    public String toString() {
        return "DedicatedChannelInfo{" +
                "issueKey='" + issueKey + '\'' +
                ", channelId='" + channelId + '\'' +
                ", teamId='" + teamId + '\'' +
                '}';
    }
}
