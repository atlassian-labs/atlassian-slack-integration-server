package com.atlassian.jira.plugins.slack.manager.impl;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.plugins.slack.dao.ConfigurationDAO;
import com.atlassian.jira.plugins.slack.manager.PluginConfigurationManager;
import com.atlassian.jira.plugins.slack.manager.ProjectConfigurationManager;
import com.atlassian.jira.plugins.slack.model.EventFilterType;
import com.atlassian.jira.plugins.slack.model.EventMatcherType;
import com.atlassian.jira.plugins.slack.model.ProjectConfiguration;
import com.atlassian.jira.plugins.slack.model.analytics.IssuePanelHiddenEvent;
import com.atlassian.jira.plugins.slack.model.analytics.IssuePanelShownEvent;
import com.atlassian.jira.plugins.slack.model.analytics.ProjectNotificationConfiguredEvent;
import com.atlassian.jira.plugins.slack.model.dto.ConfigurationGroupDTO;
import com.atlassian.jira.plugins.slack.model.dto.ProjectConfigurationDTO;
import com.atlassian.jira.plugins.slack.model.dto.ProjectToChannelConfigurationDTO;
import com.atlassian.jira.plugins.slack.model.event.AutoConvertEvent;
import com.atlassian.jira.plugins.slack.model.event.AutoConvertEvent.Type;
import com.atlassian.jira.plugins.slack.model.event.ConfigurationEvent.ConfigurationEventType;
import com.atlassian.jira.plugins.slack.model.event.ProjectMappingConfigurationEvent;
import com.atlassian.jira.plugins.slack.service.notification.NotificationInfo;
import com.atlassian.jira.plugins.slack.service.task.TaskBuilder;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugins.slack.analytics.AnalyticsContext;
import com.atlassian.plugins.slack.analytics.AnalyticsContextProvider;
import com.atlassian.plugins.slack.api.ConversationKey;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.client.ConversationLoaderHelper;
import com.atlassian.plugins.slack.api.client.ConversationsAndLinks;
import com.atlassian.plugins.slack.api.client.SlackClient;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.atlassian.plugins.slack.api.notification.Verbosity;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.plugins.slack.rest.model.SlackChannelDTO;
import com.atlassian.plugins.slack.util.AsyncExecutor;
import com.github.seratch.jslack.api.model.Conversation;
import com.google.common.base.Strings;
import com.google.common.collect.Ordering;
import io.atlassian.fugue.Either;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Slf4j
public class DefaultProjectConfigurationManager implements ProjectConfigurationManager {
    final static String PROJECT_AUTOCONVERT_ENABLED = "project.autoconvert.enabled.";
    final static String ISSUE_PANEL_HIDDEN = "issue.panel.hidden.";
    final static String SEND_RESTRICTED_COMMENTS_TO_DEDICATED = "send.restricted.comments.to.dedicated.";
    final static String CONFIGURATION_OWNER = "OWNER";
    final static String VERBOSITY = "VERBOSITY";
    public final static String IS_MUTED = "IS_MUTED";
    public static final String SKIP_RESTRICTED_COMMENTS = "SKIP_RESTRICTED_COMMENTS";

    private final ConfigurationDAO configurationDAO;
    private final AsyncExecutor asyncExecutor;
    private final TaskBuilder taskBuilder;
    private final EventPublisher eventPublisher;
    private final ProjectManager projectManager;
    private final PluginConfigurationManager pluginConfigurationManager;
    private final SlackClientProvider slackClientProvider;
    private final SlackLinkManager slackLinkManager;
    private final ConversationLoaderHelper conversationLoaderHelper;
    private final AnalyticsContextProvider analyticsContextProvider;

    @Override
    public boolean isAnyProjectConfigured() {
        return configurationDAO.isAnyProjectConfigured();
    }

    @Override
    public ProjectToChannelConfigurationDTO getConfiguration(final long projectId) {
        final Project project = projectManager.getProjectObj(projectId);
        final Collection<ProjectConfigurationDTO> config = findProjectConfigurations(Objects.requireNonNull(project));
        return toProjectToChannelConfigurationDTO(project, config);
    }

    @Override
    public List<ProjectToChannelConfigurationDTO> getConfigurations(final int startIndex, final int maxConfigurations) {
        final Collection<ProjectConfiguration> projectConfigurations = configurationDAO.getProjectConfigurations(
                startIndex, maxConfigurations);
        final Collection<ProjectConfigurationDTO> configs = toProjectConfigurationDTOs(projectConfigurations);
        if (configs.isEmpty()) {
            return Collections.emptyList();
        }

        final Map<Long, Collection<ProjectConfigurationDTO>> configsPerProject = new LinkedHashMap<>();

        for (ProjectConfigurationDTO config : configs) {
            Collection<ProjectConfigurationDTO> projectConfigs = configsPerProject
                    .computeIfAbsent(config.getProjectId(), k -> new ArrayList<>());
            projectConfigs.add(config);
        }

        final List<ProjectToChannelConfigurationDTO> results = new ArrayList<>(configsPerProject.size());
        for (Map.Entry<Long, Collection<ProjectConfigurationDTO>> kvp : configsPerProject.entrySet()) {
            final Project project = projectManager.getProjectObj(kvp.getKey());
            results.add(toProjectToChannelConfigurationDTO(project, kvp.getValue()));
        }

        return results;
    }

    private ProjectToChannelConfigurationDTO toProjectToChannelConfigurationDTO(
            final Project project,
            final Collection<ProjectConfigurationDTO> config) {
        Map<SlackChannelDTO, List<ConfigurationGroupDTO>> configuration = new LinkedHashMap<>();

        Map<String, ConfigurationGroupDTO> configurationGroupByUuid = new LinkedHashMap<>();

        long projectId = 0;
        for (ProjectConfigurationDTO projectConfigurationDTO : config) {
            projectId = projectConfigurationDTO.getProjectId();
            final SlackChannelDTO channelDTO = new SlackChannelDTO(
                    projectConfigurationDTO.getTeamId(),
                    projectConfigurationDTO.getTeamName(),
                    projectConfigurationDTO.getChannelId(),
                    projectConfigurationDTO.getChannelName(),
                    false);

            List<ConfigurationGroupDTO> configGroups = configuration.computeIfAbsent(channelDTO, k -> new ArrayList<>());

            ConfigurationGroupDTO configurationGroupDTO = configurationGroupByUuid.get(
                    projectConfigurationDTO.getConfigurationGroupId());
            if (configurationGroupDTO == null) {
                configurationGroupDTO = new ConfigurationGroupDTO(projectConfigurationDTO.getConfigurationGroupId());
                configurationGroupByUuid.put(projectConfigurationDTO.getConfigurationGroupId(), configurationGroupDTO);
                configGroups.add(configurationGroupDTO);
            }

            if (projectConfigurationDTO.getName() != null) {
                configurationGroupDTO.add(projectConfigurationDTO);
            }
        }

        return new ProjectToChannelConfigurationDTO(projectId, project.getKey(), project.getName(), configuration);
    }

    @Override
    public List<ProjectConfiguration> getMutedProjectConfigurations() {
        return configurationDAO.findMuted();
    }

    @Override
    public Set<Project> getAllProjectsByChannel(final ConversationKey conversationKey) {
        final Collection<ProjectConfiguration> configs = configurationDAO.findByChannel(conversationKey);
        return configs.stream()
                .map(config -> projectManager.getProjectObj(config.getProjectId()))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Project> getProjectsByTeamId(final String teamId) {
        final List<ProjectConfiguration> configs = configurationDAO.findByTeam(teamId);
        return configs.stream()
                .map(config -> projectManager.getProjectObj(config.getProjectId()))
                .collect(Collectors.toSet());
    }

    @Override
    public void deleteProjectConfiguration(final ProjectConfigurationDTO configurationDTO, final ApplicationUser user) {
        boolean usedToHaveMatchers = hasEventMatcher(configurationDTO.getProjectId(), configurationDTO.getChannelId());
        configurationDAO.deleteProjectConfiguration(configurationDTO);
        boolean hasMatchers = hasEventMatcher(configurationDTO.getProjectId(), configurationDTO.getChannelId());

        // VERBOSITY is also handled here; ignore it; log only real notifications
        if (isNotificationDto(configurationDTO)) {
            eventPublisher.publish(new ProjectNotificationConfiguredEvent(analyticsContextProvider.byTeamIdAndUserKey(
                    configurationDTO.getTeamId(), user.getKey()), configurationDTO.getName(), false));
        }

        publishProjectConfigurationEvent(configurationDTO, usedToHaveMatchers, hasMatchers, user);
    }

    @Override
    public void deleteProjectConfigurationGroup(final ProjectConfigurationDTO configurationDTO, final ApplicationUser user) {
        boolean usedToHaveMatchers = hasEventMatcher(configurationDTO.getProjectId(), configurationDTO.getChannelId());
        configurationDAO.deleteProjectConfigurationGroup(configurationDTO);
        boolean hasMatchers = hasEventMatcher(configurationDTO.getProjectId(), configurationDTO.getChannelId());
        publishProjectConfigurationEvent(configurationDTO, usedToHaveMatchers, hasMatchers, user);
    }

    @Override
    public void deleteProjectConfigurationsByChannelId(final ConversationKey conversationKey) {
        Map<String, List<ProjectConfiguration>> configurationsByChannelId = findProjectConfigurationsByChannelId(conversationKey);
        configurationsByChannelId.forEach((groupId, configurations) -> {
            // configurationDAO#deleteProjectConfigurationGroup requires only project id, channel id, configuration group
            configurations.stream()
                    .map(config -> ProjectConfigurationDTO.builder()
                            .setTeamId(config.getTeamId()) //teamId is required to build the DTO
                            .setChannelId(config.getChannelId())
                            .setProjectId(config.getProjectId())
                            .setConfigurationGroupId(config.getConfigurationGroupId())
                            .build())
                    .findAny()
                    .ifPresent(config -> deleteProjectConfigurationGroup(config, null));
        });
    }

    @Override
    public void updatedOwnerIfNeeded(final ProjectConfiguration configuration, final String userKey) {
        final Either<Throwable, SlackClient> slackClient = slackClientProvider.withTeamId(configuration.getTeamId());
        if (!slackClient.isRight()) {
            return;
        }

        // check current owner user has access to conversation, if any
        final Optional<String> owner = getOwner(configuration);
        boolean shouldUpdateToken = !owner.isPresent() || !slackClient.right().get()
                .withUserTokenIfAvailable(owner.get())
                .map(client -> client.getConversationsInfo(configuration.getChannelId()))
                .filter(Either::isRight)
                .isPresent();

        if (shouldUpdateToken) {
            // check new user has access to conversation
            boolean hasGoodToken = slackClient.right().get()
                    .withUserTokenIfAvailable(userKey)
                    .map(client -> client.getConversationsInfo(configuration.getChannelId()))
                    .filter(Either::isRight)
                    .isPresent();
            if (hasGoodToken) {
                final ProjectConfigurationDTO configToSave = ProjectConfigurationDTO.builder(configuration)
                        .setName(CONFIGURATION_OWNER)
                        .setValue(userKey)
                        .build();
                if (owner.isPresent()) {
                    configurationDAO.updateProjectConfiguration(configToSave);
                } else {
                    configurationDAO.insertProjectConfiguration(configToSave);
                }
            }
        }
    }

    @Override
    public Optional<String> getOwner(final ProjectConfiguration configuration) {
        return configurationDAO
                .findByProjectConfigurationGroupId(configuration.getProjectId(), configuration.getConfigurationGroupId())
                .stream()
                .filter(config -> CONFIGURATION_OWNER.equals(config.getName()))
                .map(ProjectConfiguration::getValue)
                // users report that sometimes project configurations don't have an owner; it caused an exception here
                // using a null check here leads to an empty Optional being returned, that allows to handle that case gracefully
                .filter(Objects::nonNull)
                .findAny();
    }

    @Override
    public Optional<Verbosity> getVerbosity(final ProjectConfiguration configuration) {
        return configurationDAO
                .findByProjectConfigurationGroupId(configuration.getProjectId(), configuration.getConfigurationGroupId())
                .stream()
                .filter(config -> VERBOSITY.equals(config.getName()))
                .map(ProjectConfiguration::getValue)
                .filter(StringUtils::isNotBlank)
                .findAny()
                .map(Verbosity::valueOf);
    }

    @Override
    public ProjectConfigurationDTO insertProjectConfiguration(final ProjectConfigurationDTO configurationDTO,
                                                              final ApplicationUser user) {
        boolean usedToHaveMatchers = hasEventMatcher(configurationDTO.getProjectId(), configurationDTO.getChannelId());
        configurationDAO.insertProjectConfiguration(configurationDTO);

        // VERBOSITY is also handled here; ignore it; log only real notifications
        if (isNotificationDto(configurationDTO)) {
            eventPublisher.publish(new ProjectNotificationConfiguredEvent(analyticsContextProvider.byTeamIdAndUserKey(
                    configurationDTO.getTeamId(), user.getKey()), configurationDTO.getName(), true));
        }

        boolean hasMatchers = hasEventMatcher(configurationDTO.getProjectId(), configurationDTO.getChannelId());
        publishProjectConfigurationEvent(configurationDTO, usedToHaveMatchers, hasMatchers, user);

        if (!usedToHaveMatchers && !hasMatchers) {
            slackClientProvider.withTeamId(configurationDTO.getTeamId())
                    // if user hasn't a confirmed account
                    .flatMap(client -> client.withUserToken(user.getKey()))
                    .forEach(client -> client.selfInviteToConversation(configurationDTO.getChannelId()));
        }

        return ProjectConfigurationDTO.builder(configurationDTO).build();
    }

    private boolean isNotificationDto(ProjectConfigurationDTO configurationDTO) {
        return StringUtils.startsWithAny(configurationDTO.getName(), "MATCHER:", "FILTER:");
    }

    @Override
    public void updateProjectConfiguration(final ProjectConfigurationDTO configurationDTO) {
        configurationDAO.updateProjectConfiguration(configurationDTO);
    }

    public boolean isProjectAutoConvertEnabled(final Project project) {
        Object value = pluginConfigurationManager.getSettings().get(PROJECT_AUTOCONVERT_ENABLED + project.getId());

        // If nothing is set the default behaviour is the correct one
        if (value == null) {
            return pluginConfigurationManager.isGlobalAutoConvertEnabled();
        }

        return Boolean.valueOf((String) value);
    }

    public void setProjectAutoConvertEnabled(final Project project, final boolean enabled) {
        pluginConfigurationManager.getSettings()
                .put(PROJECT_AUTOCONVERT_ENABLED + project.getId(), Boolean.toString(enabled));

        eventPublisher.publish(new AutoConvertEvent(analyticsContextProvider.current(), Type.PROJECT, enabled));
    }


    private Collection<ProjectConfigurationDTO> findProjectConfigurations(@Nonnull final Project project) {
        final Collection<ProjectConfiguration> projectConfigurations = configurationDAO.findByProjectId(project.getId());
        return toProjectConfigurationDTOs(projectConfigurations);
    }

    private List<ProjectConfigurationDTO> toProjectConfigurationDTOs(
            final Collection<ProjectConfiguration> projectConfigurations
    ) {
        final ConversationsAndLinks conversationsAndLinks = conversationLoaderHelper.conversationsAndLinksById(
                projectConfigurations.stream()
                        .filter(projectConfiguration -> projectConfiguration.getName() == null)
                        .collect(Collectors.toList()),
                config -> new ConversationKey(config.getTeamId(), config.getChannelId()),
                (baseClient, channel) -> baseClient.withRemoteUserTokenIfAvailable().flatMap(
                        client -> client.getConversationsInfo(channel.getChannelId()).toOptional()));

        final List<ProjectConfigurationDTO> channelMappings = new ArrayList<>(projectConfigurations.size());
        for (ProjectConfiguration projectConfiguration : projectConfigurations) {
            final Project projectObj = projectManager.getProjectObj(projectConfiguration.getProjectId());
            if (projectObj == null) {
                continue;
            }

            final String teamId = projectConfiguration.getTeamId();
            final Optional<SlackLink> link = conversationsAndLinks.link(teamId);
            if (!link.isPresent()) {
                continue;
            }

            final String channelId = projectConfiguration.getChannelId();
            final ConversationKey conversationKey = new ConversationKey(teamId, channelId);
            final Optional<Conversation> conversation = conversationsAndLinks.conversation(conversationKey);
            final String channelName = conversation.map(Conversation::getName).orElseGet(() -> "id:" + channelId);

            final ProjectConfigurationDTO.Builder configBuilder =
                    ProjectConfigurationDTO.builder()
                            .setTeamId(teamId)
                            .setTeamName(link.get().getTeamName())
                            .setChannelId(channelId)
                            .setChannelName(channelName)
                            .setProjectId(projectObj.getId())
                            .setProjectKey(projectObj.getKey())
                            .setProjectName(projectObj.getName())
                            .setName(projectConfiguration.getName())
                            .setValue(projectConfiguration.getValue())
                            .setMatcher(EventMatcherType.fromName(projectConfiguration.getName()),
                                    projectConfiguration.getValue())
                            .setFilter(EventFilterType.fromName(projectConfiguration.getName()),
                                    projectConfiguration.getValue());

            if (!Strings.isNullOrEmpty(projectConfiguration.getConfigurationGroupId())) {
                configBuilder.setConfigurationGroupId(projectConfiguration.getConfigurationGroupId());
            }

            channelMappings.add(configBuilder.build());
        }

        channelMappings.sort(Ordering
                .from(String.CASE_INSENSITIVE_ORDER)
                .onResultOf(ProjectConfigurationDTO::getProjectName)
                .compound(Ordering.from(String.CASE_INSENSITIVE_ORDER)
                        .onResultOf(ProjectConfigurationDTO::getChannelName))
                .compound(Ordering.from(String.CASE_INSENSITIVE_ORDER)
                        .onResultOf(ProjectConfigurationDTO::getConfigurationGroupId)));

        return channelMappings;
    }

    private boolean hasEventMatcher(final long projectId, final String channelId) {
        return configurationDAO.findByProjectId(projectId).stream()
                .anyMatch(input -> EventMatcherType.fromName(input.getName()) != null && input.getChannelId().equals(channelId));
    }

    private void publishProjectConfigurationEvent(final ProjectConfigurationDTO configurationDTO,
                                                  final boolean usedToHaveMatchers,
                                                  final boolean hasMatchers,
                                                  final @Nullable ApplicationUser user) {
        if (usedToHaveMatchers == hasMatchers) {
            return;
        }

        final ConfigurationEventType configurationEventType = hasMatchers ? ConfigurationEventType.CHANNEL_LINKED :
                ConfigurationEventType.CHANNEL_UNLINKED;

        // We assume guest access is on if we fail to fetch details of the channel.
        AnalyticsContext context = user == null
                ? analyticsContextProvider.byTeamId(configurationDTO.getTeamId())
                : analyticsContextProvider.byTeamIdAndUserKey(configurationDTO.getTeamId(), user.getKey());
        final ProjectMappingConfigurationEvent event = ProjectMappingConfigurationEvent.builder()
                .eventType(configurationEventType)
                .projectId(configurationDTO.getProjectId())
                .projectKey(configurationDTO.getProjectKey())
                .projectName(configurationDTO.getProjectName())
                .teamId(configurationDTO.getTeamId())
                .channelId(configurationDTO.getChannelId())
                .user(user)
                .context(context)
                .build();

        slackLinkManager.getLinkByTeamId(configurationDTO.getTeamId()).forEach(link -> {
            NotificationInfo notificationInfo = new NotificationInfo(link, event.getChannelId(), null, null,
                    getOwner(configurationDTO).orElse(null),
                    getVerbosity(configurationDTO).orElse(Verbosity.EXTENDED));
            asyncExecutor.run(taskBuilder.newSendNotificationTask(event, notificationInfo, asyncExecutor));

            eventPublisher.publish(event.getAnalyticsEvent());
        });
    }

    @Override
    public void muteProjectConfigurationsByChannelId(final ConversationKey conversationKey) {
        Map<String, List<ProjectConfiguration>> configurationGroups = findProjectConfigurationsByChannelId(conversationKey);
        List<ProjectConfiguration> configurationsToMute = configurationGroups.values().stream()
                .filter(projectConfigs -> projectConfigs.stream().noneMatch(config -> IS_MUTED.equals(config.getName())))
                .map(projectConfigs -> projectConfigs.stream().filter(config -> config.getName() == null).findAny())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
        configurationsToMute.stream()
                .map(config -> ProjectConfigurationDTO.builder(config)
                        .setName(IS_MUTED)
                        .setValue(Boolean.TRUE.toString())
                        .build())
                .peek(config -> log.debug("Muting configuration of group {} connected to channel {}",
                        config.getConfigurationGroupId(), config.getChannelId()))
                .forEach(configurationDAO::insertProjectConfiguration);
    }

    private Map<String, List<ProjectConfiguration>> findProjectConfigurationsByChannelId(final ConversationKey conversationKey) {
        return configurationDAO.findByChannel(conversationKey).stream()
                .collect(Collectors.groupingBy(ProjectConfiguration::getConfigurationGroupId));
    }

    @Override
    public void unmuteProjectConfigurationsByChannelId(final ConversationKey conversationKey) {
        configurationDAO.findByChannel(conversationKey).stream()
                .filter(config -> IS_MUTED.equals(config.getName()))
                .peek(config -> log.debug("Unmuting configuration of group {} connected to channel {}",
                        config.getConfigurationGroupId(), config.getChannelId()))
                .forEach(configurationDAO::deleteProjectConfiguration);
    }

    @Override
    public boolean isIssuePanelHidden(final Project project) {
        Object value = pluginConfigurationManager.getSettings().get(ISSUE_PANEL_HIDDEN + project.getId());
        return value != null && Boolean.valueOf((String) value);
    }

    @Override
    public void setIssuePanelHidden(final Project project, final boolean hide) {
        pluginConfigurationManager.getSettings().put(ISSUE_PANEL_HIDDEN + project.getId(), Boolean.toString(hide));
        if (hide) {
            eventPublisher.publish(new IssuePanelHiddenEvent(analyticsContextProvider.current(), project.getId()));
        } else {
            eventPublisher.publish(new IssuePanelShownEvent(analyticsContextProvider.current(), project.getId()));
        }
    }

    @Override
    public boolean shouldSendRestrictedCommentsToDedicatedChannels(final Project project) {
        Object value = pluginConfigurationManager.getSettings().get(SEND_RESTRICTED_COMMENTS_TO_DEDICATED + project.getId());
        return value != null && Boolean.valueOf((String) value);
    }

    @Override
    public void setSendRestrictedCommentsToDedicatedChannels(final Project project, final boolean send) {
        pluginConfigurationManager.getSettings().put(SEND_RESTRICTED_COMMENTS_TO_DEDICATED + project.getId(), Boolean.toString(send));
    }
}
