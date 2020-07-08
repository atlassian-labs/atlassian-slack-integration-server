package com.atlassian.jira.plugins.slack.model.dto;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.codehaus.jackson.annotate.JsonAutoDetect.Visibility.ANY;

/**
 * This object contains the configuration from a specific group
 */
@SuppressWarnings("unused")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = ANY)
public class ConfigurationGroupDTO extends BaseDTO {
    private final String configurationGroupId;
    private final Map<String, ProjectConfigurationDTO> settings = new HashMap<>();

    public ConfigurationGroupDTO(String configurationGroupId) {
        this.configurationGroupId = configurationGroupId;
    }

    public String getConfigurationGroupId() {
        return configurationGroupId;
    }

    public Map<String, ProjectConfigurationDTO> getSettings() {
        return Collections.unmodifiableMap(settings);
    }

    public void add(ProjectConfigurationDTO setting) {
        settings.put(setting.getName(), setting);
    }
}
