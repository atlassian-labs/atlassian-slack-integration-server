package com.atlassian.jira.plugins.slack.model.dto;

import com.atlassian.jira.plugins.slack.model.EventFilterType;
import com.atlassian.jira.plugins.slack.model.EventMatcherType;
import com.atlassian.jira.plugins.slack.model.ProjectConfiguration;
import com.atlassian.jira.project.Project;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;

import java.util.UUID;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * This object will represent a configuration item that maps a project to a channel.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = ANY)
public class ProjectConfigurationDTO extends BaseDTO implements ProjectConfiguration {
    private final long projectId;
    private final String teamId;
    private final String teamName;
    private final String channelId;
    private final String projectKey;
    private final String channelName;
    private final String projectName;
    private final String configurationGroupId;
    private final String name;
    private final String value;

    @JsonCreator
    private ProjectConfigurationDTO(@JsonProperty("projectId") final long projectId,
                                    @JsonProperty("teamId") final String teamId,
                                    @JsonProperty("teamName") final String teamName,
                                    @JsonProperty("channelId") final String channelId,
                                    @JsonProperty("channelName") final String channelName,
                                    @JsonProperty("projectKey") final String projectKey,
                                    @JsonProperty("projectName") final String projectName,
                                    @JsonProperty("configurationGroupId") final String configurationGroupId,
                                    @JsonProperty("name") final String name,
                                    @JsonProperty("value") final String value) {
        checkArgument(projectId >= 0, "Invalid projectId");
        checkArgument(!isNullOrEmpty(teamId), "teamId can't be blank/null");
        checkArgument(!isNullOrEmpty(channelId), "channelId can't be blank/null");

        this.teamName = teamName;
        this.projectId = projectId;
        this.teamId = teamId;
        this.channelId = channelId;
        this.channelName = channelName;
        this.projectKey = projectKey;
        this.projectName = projectName;
        this.configurationGroupId = configurationGroupId;
        this.name = name;
        this.value = value;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(ProjectConfigurationDTO copy) {
        return new Builder(copy);
    }

    public static Builder builder(ConfigurationMigrationDTO copy) {
        return new Builder(copy);
    }

    public static Builder builder(ProjectConfiguration copy) {
        return new Builder(copy);
    }

    @Override
    public long getProjectId() {
        return projectId;
    }

    @Override
    public String getTeamId() {
        return teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    @Override
    public String getChannelId() {
        return channelId;
    }

    @SuppressWarnings("unused")
    public String getChannelName() {
        return channelName;
    }

    @SuppressWarnings("unused")
    public String getProjectKey() {
        return projectKey;
    }

    @SuppressWarnings("unused")
    public String getProjectName() {
        return projectName;
    }

    @Override
    public String getConfigurationGroupId() {
        return configurationGroupId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getValue() {
        return value;
    }

    @JsonIgnore
    public EventMatcherType getMatcher() {
        return EventMatcherType.fromName(name);
    }

    @JsonIgnore
    public EventFilterType getFilter() {
        return EventFilterType.fromName(name);
    }

    public static class Builder {
        private long projectId = NON_EXISTENT_ID;
        private String teamId;
        private String teamName;
        private String channelId;
        private String channelName;
        private String projectKey;
        private String projectName;
        private String configurationGroupId = UUID.randomUUID().toString();
        private String name;
        private String value;

        public Builder() {
        }

        public Builder(ConfigurationMigrationDTO original) {
            this.teamId = original.getTeamId();
            this.channelId = original.getChannelId();
            this.projectKey = original.getProjectKey();
            this.projectId = original.getProjectId();
        }

        public Builder(ProjectConfigurationDTO original) {
            this((ProjectConfiguration) original);
            this.teamName = original.getTeamName();
            this.channelName = original.getChannelName();
            this.projectKey = original.getProjectKey();
            this.projectName = original.getProjectName();
        }

        public Builder(ProjectConfiguration original) {
            this.projectId = original.getProjectId();
            this.teamId = original.getTeamId();
            this.channelId = original.getChannelId();
            this.configurationGroupId = !Strings.isNullOrEmpty(original.getConfigurationGroupId()) ?
                    original.getConfigurationGroupId() : this.configurationGroupId;
            this.name = original.getName();
            this.value = original.getValue();
        }

        public Builder setProject(Project project) {
            return setProjectId(project.getId())
                    .setProjectKey(project.getKey())
                    .setProjectName(project.getName());
        }

        public Builder setProjectId(long projectId) {
            this.projectId = projectId;
            return this;
        }

        public Builder setChannelId(String channelId) {
            this.channelId = channelId;
            return this;
        }

        public Builder setTeamName(String teamName) {
            this.teamName = teamName;
            return this;
        }

        public Builder setTeamId(String teamId) {
            this.teamId = teamId;
            return this;
        }

        public Builder setChannelName(String channelName) {
            this.channelName = channelName;
            return this;
        }

        public Builder setProjectKey(String projectKey) {
            this.projectKey = projectKey;
            return this;
        }

        public Builder setProjectName(String projectName) {
            this.projectName = projectName;
            return this;
        }

        public Builder setConfigurationGroupId(String configurationGroupId) {
            this.configurationGroupId = configurationGroupId;
            return this;
        }

        public Builder setMatcher(EventMatcherType matcher, String value) {
            if (matcher != null) {
                this.name = matcher.getDbKey();
                this.value = value;
            }
            return this;
        }

        public Builder setFilter(EventFilterType filter, String value) {
            if (filter != null) {
                this.name = filter.getDbKey();
                this.value = value;
            }
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }


        public Builder setValue(String value) {
            this.value = value;
            return this;
        }

        public ProjectConfigurationDTO build() {
            return new ProjectConfigurationDTO(projectId, teamId, teamName, channelId, channelName, projectKey,
                    projectName, configurationGroupId, name, value);
        }
    }
}
