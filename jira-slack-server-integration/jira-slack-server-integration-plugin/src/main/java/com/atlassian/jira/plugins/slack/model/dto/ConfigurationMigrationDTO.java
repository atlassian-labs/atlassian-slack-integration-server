package com.atlassian.jira.plugins.slack.model.dto;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.Map;

import static org.codehaus.jackson.annotate.JsonAutoDetect.Visibility.ANY;


@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = ANY)
public class ConfigurationMigrationDTO {
    private final long projectId;
    private final String teamId;
    private final String channelId;
    private final String projectKey;
    private final Map<String, String> values;

    @JsonCreator
    private ConfigurationMigrationDTO(@JsonProperty("projectId") final long projectId,
                                      @JsonProperty("teamId") final String teamId,
                                      @JsonProperty("channelId") final String channelId,
                                      @JsonProperty("projectKey") final String projectKey,
                                      @JsonProperty("values") final Map<String, String> values) {
        this.projectId = projectId;
        this.teamId = teamId;
        this.channelId = channelId;
        this.projectKey = projectKey;
        this.values = values;
    }

    public long getProjectId() {
        return projectId;
    }

    public String getTeamId() {
        return teamId;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getProjectKey() {
        return projectKey;
    }

    public Map<String, String> getValues() {
        return values;
    }
}
