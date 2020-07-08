package com.atlassian.jira.plugins.slack.dao;

import com.atlassian.jira.plugins.slack.model.ProjectConfiguration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * This class manages the project configuration records in database.
 */
public interface ConfigurationDAO {
    /**
     * Returns whether any project is configured or not
     *
     * @return true if any project is configured, false otherwise
     */
    boolean isAnyProjectConfigured();

    /**
     * Returns project configuration with the given ID.
     *
     * @param id ID
     * @return Configuration record, or null if not found
     */
    @Nullable
    ProjectConfiguration getById(long id);

    /**
     * Returns list of project configurations related to a given project.
     *
     * @param projectId Project ID
     * @return List of configuration records
     */
    @Nonnull
    List<ProjectConfiguration> findByProjectId(long projectId);

    /**
     * Searches for all the project configurations that are mapped to a specific channelId
     *
     * @param channelId the channelId
     * @return a collection of project configurations
     */
    List<ProjectConfiguration> findByChannel(final String channelId);

    /**
     * Searches for all the project configurations that are mapped to a specific team
     */
    List<ProjectConfiguration> findByTeam(final String teamId);

    List<ProjectConfiguration> findMuted();

    /**
     * Returns list of project configurations
     *
     * @param startIndex            startIndex
     * @param maxConfigurationGroup maximum number of group to return
     * @return List of configuration records
     */
    @Nonnull
    List<ProjectConfiguration> getProjectConfigurations(int startIndex, int maxConfigurationGroup);

    /**
     * Inserts a new project configuration record.
     *
     * @param configuration Project Configuration
     * @return the project configuration
     */
    ProjectConfiguration insertProjectConfiguration(@Nonnull ProjectConfiguration configuration);

    /**
     * Updates an existing project configuration record.
     *
     * @param id            Identifier
     * @param configuration Project Configuration
     */
    void updateProjectConfiguration(long id, @Nonnull ProjectConfiguration configuration);

    /**
     * Updates an existing project configuration record.
     *
     * @param configuration Project Configuration
     */
    void updateProjectConfiguration(@Nonnull ProjectConfiguration configuration);

    /**
     * Deletes project configuration from the database
     *
     * @param configuration Project Configuration
     */
    void deleteProjectConfiguration(@Nonnull ProjectConfiguration configuration);


    /**
     * Searches for a configuration records of a specific group. We pass the project ID as well, so that we can use the
     * project cache and retrieve records quickly.
     *
     * @param projectId the project id
     * @param groupId   the group id
     * @return the list of configurations
     */
    List<ProjectConfiguration> findByProjectConfigurationGroupId(long projectId, String groupId);

    void deleteProjectConfigurationGroup(@Nonnull ProjectConfiguration configuration);

    /**
     * Deletes all the configuration from the table
     *
     * @return amount of rows deleted
     */
    int deleteAllConfigurations(String teamId);
}
