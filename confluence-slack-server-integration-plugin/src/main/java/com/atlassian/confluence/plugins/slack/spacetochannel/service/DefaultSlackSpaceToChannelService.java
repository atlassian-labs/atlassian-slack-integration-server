package com.atlassian.confluence.plugins.slack.spacetochannel.service;

import com.atlassian.confluence.plugins.slack.spacetochannel.ao.AOEntityToChannelMapping;
import com.atlassian.confluence.plugins.slack.spacetochannel.ao.EntityToChannelMappingManager;
import com.atlassian.confluence.plugins.slack.spacetochannel.configuration.SlackChannelDefinition;
import com.atlassian.confluence.plugins.slack.spacetochannel.configuration.SpaceToChannelConfiguration;
import com.atlassian.confluence.plugins.slack.spacetochannel.configuration.SpaceToChannelSettings;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.spaces.SpaceManager;
import com.atlassian.plugins.slack.api.ConversationKey;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.client.ConversationLoaderHelper;
import com.atlassian.plugins.slack.api.client.ConversationsAndLinks;
import com.atlassian.plugins.slack.api.client.RetryLoaderHelper;
import com.atlassian.plugins.slack.api.descriptor.NotificationTypeService;
import com.atlassian.plugins.slack.api.notification.NotificationType;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.plugins.slack.settings.SlackSettingService;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.atlassian.sal.api.user.UserManager;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class DefaultSlackSpaceToChannelService implements SlackSpaceToChannelService {
    private static final Logger logger = LoggerFactory.getLogger(DefaultSlackSpaceToChannelService.class);

    private final SpaceManager spaceManager;
    private final EntityToChannelMappingManager entityToChannelMappingManager;
    private final SlackLinkManager slackLinkManager;
    private final NotificationTypeService notificationTypeService;
    private final ConversationLoaderHelper conversationLoaderHelper;
    private final SlackSettingService slackSettingService;
    private final UserManager userManager;
    private final TransactionTemplate transactionTemplate;

    @Autowired
    public DefaultSlackSpaceToChannelService(
            final SpaceManager spaceManager,
            final EntityToChannelMappingManager entityToChannelMappingManager,
            final SlackLinkManager slackLinkManager,
            final NotificationTypeService notificationTypeService,
            final ConversationLoaderHelper conversationLoaderHelper,
            final RetryLoaderHelper retryLoaderHelper,
            final SlackSettingService slackSettingService,
            final UserManager userManager,
            final TransactionTemplate transactionTemplate) {
        this.spaceManager = spaceManager;
        this.entityToChannelMappingManager = entityToChannelMappingManager;
        this.slackLinkManager = slackLinkManager;
        this.notificationTypeService = notificationTypeService;
        this.conversationLoaderHelper = conversationLoaderHelper;
        this.slackSettingService = slackSettingService;
        this.userManager = userManager;
        this.transactionTemplate = transactionTemplate;
    }

    private Function<ConversationKey, Optional<SlackChannelDefinition>> getChannelProvider(
            final List<AOEntityToChannelMapping> mappings) {
        // remote user key is stored here because code below is async and can't access request context
        final String userKey = userManager.getRemoteUserKey().getStringValue();
        final ConversationsAndLinks conversationsAndLinks = conversationLoaderHelper.conversationsAndLinksById(
                mappings,
                mapping -> new ConversationKey(mapping.getTeamId(), mapping.getChannelId()),
                // transaction is needed because this runs async and won't have connection bounded
                (slackClient, channel) -> transactionTemplate.execute(() -> slackClient.withUserTokenIfAvailable(userKey)
                        .flatMap(client -> client.getConversationsInfo(channel.getChannelId()).toOptional())));

        return conversationKey -> {
            Optional<SlackLink> slackLink = conversationsAndLinks.linkByConversationKey(conversationKey);
            return slackLink
                    .map(link -> conversationsAndLinks.conversation(conversationKey)
                            .map(conversation -> new SlackChannelDefinition(
                                    link.getTeamName(),
                                    link.getTeamId(),
                                    conversation.getName(),
                                    conversation.getId(),
                                    conversation.isPrivate(),
                                    slackSettingService.isChannelMuted(conversationKey)))
                            .orElseGet(() -> new SlackChannelDefinition(
                                    link.getTeamName(),
                                    link.getTeamId(),
                                    "id:" + conversationKey.getChannelId(),
                                    conversationKey.getChannelId(),
                                    true,
                                    slackSettingService.isChannelMuted(conversationKey))));
        };
    }

    @Override
    public List<SpaceToChannelConfiguration> getAllSpaceToChannelLinks() {
        if (slackLinkManager.isAnyLinkDefined()) {
            final List<AOEntityToChannelMapping> mappings = entityToChannelMappingManager.getAll();
            final Function<ConversationKey, Optional<SlackChannelDefinition>> channelProvider = getChannelProvider(mappings);
            final Map<String, SpaceToChannelConfiguration.Builder> spaceBuildersBySpaceKey = new HashMap<>();

            for (AOEntityToChannelMapping mapping : mappings) {
                String spaceKey = mapping.getEntityKey();
                SpaceToChannelConfiguration.Builder builder = spaceBuildersBySpaceKey.get(spaceKey);
                if (builder == null) {
                    Space space = spaceManager.getSpace(spaceKey);
                    if (space != null) {
                        builder = newConfigBuilder(space, channelProvider);
                        spaceBuildersBySpaceKey.put(spaceKey, builder);
                    }
                }
                if (builder != null) {
                    addAOEntityChannelMappingsToBuilder(mapping, builder.getSettingsBuilder(new ConversationKey(mapping.getTeamId(), mapping.getChannelId())));
                }
            }

            return spaceBuildersBySpaceKey.values().stream()
                    .map(SpaceToChannelConfiguration.Builder::build)
                    .collect(Collectors.toList());
        } else {
            // There is no link so just return an empty iterable
            return ImmutableList.of();
        }
    }

    @Override
    public List<SpaceToChannelConfiguration> getAllSpaceToChannelConfigurations() {
        return ImmutableList.copyOf(getAllSpaceToChannelLinks());
    }

    @Override
    public SpaceToChannelConfiguration getSpaceToChannelConfiguration(final String spaceKey) {
        Space space = spaceManager.getSpace(spaceKey);
        final List<AOEntityToChannelMapping> mappings = entityToChannelMappingManager.getForEntity(spaceKey);
        final Function<ConversationKey, Optional<SlackChannelDefinition>> channelProvider = getChannelProvider(mappings);

        SpaceToChannelConfiguration.Builder builder = newConfigBuilder(space, channelProvider);
        addChannelMappingsToBuilder(mappings, builder);

        return builder.build();
    }

    @Override
    public Optional<SpaceToChannelSettings> getSpaceToChannelSettings(String spaceKey, String channelId) {
        SpaceToChannelSettings.Builder builder = new SpaceToChannelSettings.Builder();

        final List<AOEntityToChannelMapping> mappings = entityToChannelMappingManager.getForEntityAndChannel(spaceKey, channelId);
        if (!mappings.iterator().hasNext()) {
            return Optional.empty();
        }
        addChannelMappingsToBuilder(mappings, builder);
        return Optional.of(builder.build());
    }

    @Override
    public boolean hasSpaceToChannelConfiguration(final String spaceKey) {
        return entityToChannelMappingManager.hasConfigurationForEntity(spaceKey);
    }

    @Override
    public boolean hasMappingForEntityChannelAndType(final String entity,
                                                     final String channelId,
                                                     final NotificationType type) {
        return entityToChannelMappingManager.hasConfigurationForEntityChannelAndType(entity, channelId, type);
    }

    @Override
    public void removeNotificationsForTeam(final String teamId) {
        entityToChannelMappingManager.removeNotificationsForTeam(teamId);
    }

    @VisibleForTesting
    SpaceToChannelConfiguration.Builder newConfigBuilder(
            final Space space,
            final Function<ConversationKey, Optional<SlackChannelDefinition>> channelProvider) {
        return new SpaceToChannelConfiguration.Builder(space, channelProvider);
    }

    @Override
    public void addNotificationForSpaceAndChannel(
            final String spaceKey,
            final String owner,
            final String teamId,
            final String channelId,
            final NotificationType notificationType) {
        entityToChannelMappingManager.addNotificationForEntityAndChannel(spaceKey, owner, teamId, channelId, notificationType);
    }

    @Override
    public void removeNotificationForSpaceAndChannel(
            final String spaceKey,
            final ConversationKey conversationKey,
            final NotificationType notificationType) {
        entityToChannelMappingManager.removeNotificationForEntityAndChannel(spaceKey, conversationKey, notificationType);
    }

    public void removeNotificationsForSpaceAndChannel(
            final String spaceKey,
            final ConversationKey conversationKey) {
        entityToChannelMappingManager.removeNotificationsForEntityAndChannel(spaceKey, conversationKey);
    }

    @Override
    public void removeNotificationsForChannel(final ConversationKey conversationKey) {
        entityToChannelMappingManager.removeNotificationsForChannel(conversationKey);
    }

    @Override
    public int removeNotificationsForSpace(String spaceKey) {
        return entityToChannelMappingManager.removeNotificationsForEntity(spaceKey);
    }

    // Builder helpers for Iterables of entities (irritated entities?)

    private void addChannelMappingsToBuilder(List<AOEntityToChannelMapping> mappings, SpaceToChannelConfiguration.Builder builder) {
        for (AOEntityToChannelMapping mapping : mappings) {
            SpaceToChannelSettings.Builder settingsBuilder = builder.getSettingsBuilder(new ConversationKey(mapping.getTeamId(), mapping.getChannelId()));
            addAOEntityChannelMappingsToBuilder(mapping, settingsBuilder);
        }
    }

    private void addChannelMappingsToBuilder(List<AOEntityToChannelMapping> mappings, SpaceToChannelSettings.Builder builder) {
        for (AOEntityToChannelMapping mapping : mappings) {
            addAOEntityChannelMappingsToBuilder(mapping, builder);
        }
    }

    // Builder helper for individual entities.

    private void addAOEntityChannelMappingsToBuilder(AOEntityToChannelMapping mapping, SpaceToChannelSettings.Builder builder) {
        final String notificationTypeKey = mapping.getMessageTypeKey();
        final Optional<NotificationType> spaceToChannelNotificationOption =
                notificationTypeService.getNotificationTypeForKey(notificationTypeKey);
        if (spaceToChannelNotificationOption.isPresent()) {
            NotificationType spaceToChannelNotification = spaceToChannelNotificationOption.get();
            builder.addNotificationType(spaceToChannelNotification);
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Unable to parse SpaceToChannelNotification from '" + notificationTypeKey + "'.");
            }
        }
    }

}
