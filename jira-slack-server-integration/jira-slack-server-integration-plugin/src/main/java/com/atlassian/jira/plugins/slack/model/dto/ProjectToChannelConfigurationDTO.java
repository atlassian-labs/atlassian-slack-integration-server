package com.atlassian.jira.plugins.slack.model.dto;

import com.atlassian.plugins.slack.rest.model.SlackChannelDTO;
import com.google.common.collect.Maps;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.codehaus.jackson.annotate.JsonAutoDetect.Visibility.ANY;

/**
 * This class contains all configuration for a project:
 * <ul>
 * <li>{@link SlackChannelDTO} it is linked to</li>
 * <li>list of {@link ConfigurationGroupDTO} for each channel</li>
 * </ul>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = ANY)
public class ProjectToChannelConfigurationDTO extends BaseDTO {
    private final long projectId;
    private final String projectName;
    private final String projectKey;
    private final Map<String, SlackChannelDTO> channels;
    private final Map<String, List<ConfigurationGroupDTO>> configuration;
    private final List<String> orderedChannelIds;

    public ProjectToChannelConfigurationDTO(final long projectId,
                                            final String projectKey,
                                            final String projectName,
                                            final Map<SlackChannelDTO, List<ConfigurationGroupDTO>> configuration) {
        this.projectId = projectId;
        this.projectName = projectName;
        this.projectKey = projectKey;

        this.configuration = configuration.entrySet().stream().collect(Collectors.toMap(
                e -> e.getKey().getChannelId(),
                Map.Entry::getValue
        ));

        this.channels = Maps.uniqueIndex(configuration.keySet(), SlackChannelDTO::getChannelId);

        this.orderedChannelIds = configuration.keySet().stream()
                .map(SlackChannelDTO::getChannelId)
                .collect(Collectors.toList());
    }

    public long getProjectId() {
        return projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getProjectKey() {
        return projectKey;
    }

    public Map<String, SlackChannelDTO> getChannels() {
        return channels;
    }

    public Map<String, List<ConfigurationGroupDTO>> getConfiguration() {
        return configuration;
    }

    @SuppressWarnings("unused")
    public List<String> getOrderedChannelIds() {
        return orderedChannelIds;
    }
}
