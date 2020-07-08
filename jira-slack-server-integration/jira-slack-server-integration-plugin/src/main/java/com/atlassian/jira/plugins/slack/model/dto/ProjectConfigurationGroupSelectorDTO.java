package com.atlassian.jira.plugins.slack.model.dto;

import com.atlassian.jira.plugins.slack.model.ProjectConfigurationGroupSelector;

import javax.annotation.Nonnull;

public class ProjectConfigurationGroupSelectorDTO implements ProjectConfigurationGroupSelector {
    private final long projectId;
    private final String projectConfigurationGroupId;

    public ProjectConfigurationGroupSelectorDTO(final long projectId,
                                                @Nonnull final String projectConfigurationGroupId) {
        this.projectId = projectId;
        this.projectConfigurationGroupId = projectConfigurationGroupId;
    }

    public long getProjectId() {
        return projectId;
    }

    @Nonnull
    public String getProjectConfigurationGroupId() {
        return projectConfigurationGroupId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ProjectConfigurationGroupSelectorDTO that = (ProjectConfigurationGroupSelectorDTO) o;

        if (projectId != that.projectId) {
            return false;
        }
        if (!projectConfigurationGroupId.equals(that.projectConfigurationGroupId)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (projectId ^ (projectId >>> 32));
        result = 31 * result + projectConfigurationGroupId.hashCode();
        return result;
    }
}
