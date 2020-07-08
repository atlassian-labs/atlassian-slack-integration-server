package com.atlassian.jira.plugins.slack.manager;

import com.atlassian.jira.plugins.slack.model.ProjectConfiguration;
import com.atlassian.jira.plugins.slack.model.dto.ProjectConfigurationDTO;
import com.atlassian.jira.plugins.slack.model.dto.ProjectToChannelConfigurationDTO;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugins.slack.api.notification.Verbosity;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * This class manages Jira project configuration with Slack
 */
public interface ProjectConfigurationManager {

    /**
     * Returns whether any project is configured or not
     *
     * @return true if any project is configured, false otherwise
     */
    boolean isAnyProjectConfigured();

    /**
     * Returns the {@link ProjectToChannelConfigurationDTO} for a given project
     *
     * @param projectId ID of the project to find mappings for
     * @return project to channel configuration
     */
    ProjectToChannelConfigurationDTO getConfiguration(long projectId);

    /**
     * Returns all the projects that a channel is connected to
     *
     * @param channelId because you want the keys for the channel
     * @return a set of project keys
     */
    Set<Project> getAllProjectsByChannel(String channelId);

    /**
     * Returns projects connected to any channel in a specified team.
     *
     * @param teamId id of the team which channels belong to
     * @return set of projects
     */
    Set<Project> getProjectsByTeamId(String teamId);

    /**
     * Returns a list of {@link ProjectToChannelConfigurationDTO}
     *
     * @param startIndex        startIndex
     * @param maxConfigurations maximum number of project configuration that will be returned
     * @return list of project to channel configuration
     */
    List<ProjectToChannelConfigurationDTO> getConfigurations(int startIndex, int maxConfigurations);

    List<ProjectConfiguration> getMutedProjectConfigurations();

    void updatedOwnerIfNeeded(ProjectConfiguration configuration, String userKey);

    Optional<String> getOwner(ProjectConfiguration configuration);

    Optional<Verbosity> getVerbosity(final ProjectConfiguration configuration);

    /**
     * Inserts a new project configuration.
     *
     * @param configurationDTO new project configuration
     * @param user             the user creating the mapping
     * @return the inserted project configuration
     */
    ProjectConfigurationDTO insertProjectConfiguration(ProjectConfigurationDTO configurationDTO, ApplicationUser user);

    void updateProjectConfiguration(ProjectConfigurationDTO configurationDTO);

    /**
     * Deletes an existing project configuration.
     *
     * @param configurationDTO project configuration to delete
     */
    void deleteProjectConfiguration(ProjectConfigurationDTO configurationDTO, ApplicationUser user);

    /**
     * Deletes an entire project configuration group.
     *
     * @param configurationDTO project configuration belonging to the configuration group to delete
     */
    void deleteProjectConfigurationGroup(ProjectConfigurationDTO configurationDTO, ApplicationUser user);

    void deleteProjectConfigurationsByChannelId(String channelId);

    /**
     * Verifies if the project autoconvert is enable
     * If nothing was set then it means that the default behaviour is being used.
     *
     * @param project the project
     * @return true if autoconvert is enable, false if not
     */
    boolean isProjectAutoConvertEnabled(Project project);

    /**
     * This will override the default behaviour to the one desired by the user
     *
     * @param project the project
     * @param value   the value
     */
    void setProjectAutoConvertEnabled(Project project, boolean value);

    /**
     * Mute notifications coming from channel with specified ID.
     *
     * @param channelId channel ID to mute
     */
    void muteProjectConfigurationsByChannelId(String channelId);

    /**
     * Unmute channels which were previously muted by {@link #muteProjectConfigurationsByChannelId(String)}.
     *
     * @param channelId channel ID to unmute
     */
    void unmuteProjectConfigurationsByChannelId(String channelId);

    boolean isIssuePanelHidden(Project project);

    void setIssuePanelHidden(Project project, boolean value);

    boolean shouldSendRestrictedCommentsToDedicatedChannels(Project project);

    void setSendRestrictedCommentsToDedicatedChannels(Project project, boolean value);
}
