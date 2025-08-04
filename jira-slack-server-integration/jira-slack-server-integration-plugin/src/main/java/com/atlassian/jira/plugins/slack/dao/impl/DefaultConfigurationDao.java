package com.atlassian.jira.plugins.slack.dao.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheManager;
import com.atlassian.jira.plugins.slack.dao.ConfigurationDAO;
import com.atlassian.jira.plugins.slack.manager.impl.DefaultProjectConfigurationManager;
import com.atlassian.jira.plugins.slack.model.ProjectConfiguration;
import com.atlassian.jira.plugins.slack.model.ao.ProjectConfigurationAO;
import com.atlassian.plugins.slack.api.ConversationKey;
import com.google.common.collect.ImmutableList;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import net.java.ao.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This is default implementation of {@link com.atlassian.jira.plugins.slack.dao.ConfigurationDAO} using Active
 * Objects.
 */
@Component
public class DefaultConfigurationDao implements ConfigurationDAO {
    private final Logger logger = LoggerFactory.getLogger(DefaultConfigurationDao.class);

    private final ActiveObjects ao;

    private final Cache<Long, List<ProjectConfiguration>> cache;

    @Autowired
    public DefaultConfigurationDao(final ActiveObjects ao,
                                   final CacheManager cacheManager) {
        this.ao = ao;
        cache = cacheManager.getCache(
                DefaultConfigurationDao.class.getName() + ".slack-project-configurations",
                projectId -> Collections.unmodifiableList(Arrays.asList(ao.find(
                        ProjectConfigurationAO.class, ProjectConfigurationAO.COLUMN_PROJECT_ID + " = ?", projectId)))
        );
    }

    @Override
    public boolean isAnyProjectConfigured() {
        for (Long projectId : cache.getKeys()) {
            final Collection<ProjectConfiguration> projectConfigurations = cache.get(projectId);
            if (projectConfigurations != null && projectConfigurations.size() > 0) {
                return true;
            }
        }
        return ao.count(ProjectConfigurationAO.class) > 0;
    }

    @Nullable
    @Override
    public ProjectConfigurationAO getById(final long id) {
        ProjectConfigurationAO[] allProjectConfigurationItems = ao.find(
                ProjectConfigurationAO.class, ProjectConfigurationAO.COLUMN_ID + " = ?", id);
        return (allProjectConfigurationItems.length > 0) ? allProjectConfigurationItems[0] : null;
    }

    @Nonnull
    @Override
    public List<ProjectConfiguration> findByProjectId(final long projectId) {
        return cache.get(projectId);
    }

    @Override
    public List<ProjectConfiguration> findByChannel(final ConversationKey conversationKey) {
        ProjectConfigurationAO[] configs = ao.find(ProjectConfigurationAO.class,
                ProjectConfigurationAO.COLUMN_TEAM_ID + " = ? AND " + ProjectConfigurationAO.COLUMN_CHANNEL_ID + " = ?",
                conversationKey.getTeamId(), conversationKey.getChannelId());

        return ImmutableList.copyOf(configs);
    }

    @Override
    public List<ProjectConfiguration> findByTeam(final String teamId) {
        ProjectConfigurationAO[] configs = ao.find(ProjectConfigurationAO.class,
                ProjectConfigurationAO.COLUMN_TEAM_ID + " = ?", teamId);

        return ImmutableList.copyOf(configs);
    }

    @Override
    public List<ProjectConfiguration> findMuted() {
        ProjectConfigurationAO[] configs = ao.find(ProjectConfigurationAO.class,
                ProjectConfigurationAO.COLUMN_NAME + " = ?", DefaultProjectConfigurationManager.IS_MUTED);

        return ImmutableList.copyOf(configs);
    }

    @Nonnull
    @Override
    public List<ProjectConfiguration> getProjectConfigurations(final int startIndex, final int maxConfigurationGroup) {
        final Query query = Query.select()
                .offset(startIndex)
                .limit(maxConfigurationGroup)
                .order(ProjectConfigurationAO.COLUMN_CONFIGURATION_GROUP_ID);
        ProjectConfigurationAO[] allProjectConfigurationItems = ao.find(ProjectConfigurationAO.class, query);
        return Arrays.asList(allProjectConfigurationItems);
    }

    @Override
    public ProjectConfiguration insertProjectConfiguration(@Nonnull final ProjectConfiguration projectConfiguration) {
        try {
            final ProjectConfigurationAO projectConfigurationAO = ao.create(ProjectConfigurationAO.class,
                    getValuesMap(projectConfiguration));
            cache.remove(projectConfiguration.getProjectId());
            return projectConfigurationAO;
        } catch (RuntimeException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof SQLException) {
                logger.warn("Unique constraints are violated, falling back to update.");
                updateProjectConfiguration(projectConfiguration);
                return null;
            }
            throw exception;
        }
    }

    @Override
    public void updateProjectConfiguration(final long id, @Nonnull final ProjectConfiguration projectConfiguration) {
        final ProjectConfigurationAO existingProjectConfiguration = getById(id);
        if (existingProjectConfiguration != null) {
            updateProjectConfiguration(existingProjectConfiguration, projectConfiguration);
        }
    }

    @Override
    public void updateProjectConfiguration(@Nonnull final ProjectConfiguration projectConfiguration) {
        get(projectConfiguration).ifPresent(config -> updateProjectConfiguration(config, projectConfiguration));
    }

    private void updateProjectConfiguration(@Nonnull final ProjectConfigurationAO existingProjectConfiguration,
                                            @Nonnull final ProjectConfiguration projectConfiguration) {
        existingProjectConfiguration.setName(projectConfiguration.getName());
        existingProjectConfiguration.setValue(projectConfiguration.getValue());
        existingProjectConfiguration.setChannelId(projectConfiguration.getChannelId());
        existingProjectConfiguration.setTeamId(projectConfiguration.getTeamId());
        existingProjectConfiguration.setConfigurationGroupId(projectConfiguration.getConfigurationGroupId());
        existingProjectConfiguration.setProjectId(projectConfiguration.getProjectId());
        existingProjectConfiguration.setNameUniqueConstraint(createNameProjectGroupIdUniqueConstraint(projectConfiguration));
        existingProjectConfiguration.save();
        cache.remove(projectConfiguration.getProjectId());
    }

    @Override
    public void deleteProjectConfiguration(@Nonnull final ProjectConfiguration projectConfiguration) {
        get(projectConfiguration).ifPresent(config -> {
            ao.delete(config);
            cache.remove(config.getProjectId());
        });
    }

    @Override
    public void deleteProjectConfigurationGroup(@Nonnull final ProjectConfiguration configuration) {
        final ProjectConfigurationAO[] config = ao
                .find(ProjectConfigurationAO.class,
                        ProjectConfigurationAO.COLUMN_PROJECT_ID + " = ? AND " +
                                ProjectConfigurationAO.COLUMN_CHANNEL_ID + " = ? AND " +
                                ProjectConfigurationAO.COLUMN_CONFIGURATION_GROUP_ID + " = ?",
                        configuration.getProjectId(),
                        configuration.getChannelId(),
                        configuration.getConfigurationGroupId());

        ao.delete(config);
        cache.remove(configuration.getProjectId());
    }

    @Override
    public int deleteAllConfigurations(final String teamId) {
        try {
            return ao.deleteWithSQL(ProjectConfigurationAO.class, "TEAM_ID = ?", teamId);
        } finally {
            cache.removeAll();
        }
    }

    @Override
    public List<ProjectConfiguration> findByProjectConfigurationGroupId(final long projectId,
                                                                        @Nonnull final String groupId) {
        checkNotNull(groupId, "GroupId cannot be null when searching ProjectConfigurations");
        return cache.get(projectId).stream()
                .filter(input -> input != null && input.getConfigurationGroupId().equals(groupId))
                .collect(Collectors.toList());
    }

    private Optional<ProjectConfigurationAO> get(@Nonnull final ProjectConfiguration configuration) {
        final ProjectConfigurationAO[] config = ao.find(
                ProjectConfigurationAO.class,
                ProjectConfigurationAO.COLUMN_PROJECT_ID + " = ? AND " +
                        ProjectConfigurationAO.COLUMN_CHANNEL_ID + " = ? AND " +
                        ProjectConfigurationAO.COLUMN_CONFIGURATION_GROUP_ID + " = ? AND " +
                        ProjectConfigurationAO.COLUMN_NAME + " = ?",
                configuration.getProjectId(),
                configuration.getChannelId(),
                configuration.getConfigurationGroupId(),
                configuration.getName());

        if (config.length > 1) {
            logger.warn(
                    "Found {} project configurations with PROJECT_ID '{}' CHANNEL_ID '{}' GROUP_ID '{}' and NAME '{}'",
                    config.length,
                    configuration.getProjectId(),
                    configuration.getChannelId(),
                    configuration.getConfigurationGroupId(),
                    configuration.getName());
        }

        return Optional.ofNullable((config.length > 0) ? config[0] : null);
    }

    private static Map<String, Object> getValuesMap(final ProjectConfiguration projectConfiguration) {
        Map<String, Object> values = new HashMap<>();
        values.put(ProjectConfigurationAO.COLUMN_PROJECT_ID, projectConfiguration.getProjectId());
        values.put(ProjectConfigurationAO.COLUMN_TEAM_ID, projectConfiguration.getTeamId());
        values.put(ProjectConfigurationAO.COLUMN_CHANNEL_ID, projectConfiguration.getChannelId());
        values.put(ProjectConfigurationAO.COLUMN_CONFIGURATION_GROUP_ID,
                projectConfiguration.getConfigurationGroupId());
        values.put(ProjectConfigurationAO.COLUMN_NAME, projectConfiguration.getName());
        values.put(ProjectConfigurationAO.COLUMN_VALUE, projectConfiguration.getValue());
        values.put(ProjectConfigurationAO.COLUMN_NAME_UNIQUE_CONSTRAINT,
                createNameProjectGroupIdUniqueConstraint(projectConfiguration));
        return values;
    }

    private static String createNameProjectGroupIdUniqueConstraint(final ProjectConfiguration projectConfiguration) {
        return String.format("%s:%s", projectConfiguration.getName(), projectConfiguration.getConfigurationGroupId());
    }
}
