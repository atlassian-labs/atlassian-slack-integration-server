package com.atlassian.confluence.plugins.slack.spacetochannel.configuration;

import com.atlassian.confluence.spaces.Space;
import com.atlassian.plugins.slack.api.ConversationKey;
import com.atlassian.plugins.slack.api.notification.NotificationType;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Ordering;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public class SpaceToChannelConfiguration {
    private final Map<SlackChannelDefinition, SpaceToChannelSettings> channelDefinitionSpaceToChannelSettings;
    private final Space space;

    private SpaceToChannelConfiguration(
            final Space space,
            final Map<SlackChannelDefinition, SpaceToChannelSettings> configuration) {
        Preconditions.checkNotNull(configuration);
        Preconditions.checkNotNull(space);
        this.channelDefinitionSpaceToChannelSettings = configuration;
        this.space = space;
    }

    public Optional<Set<NotificationType>> getChannelConfiguration(
            final SlackChannelDefinition channel) {
        SpaceToChannelSettings settings = channelDefinitionSpaceToChannelSettings.get(channel);
        if (settings == null) {
            return Optional.empty();
        }
        Set<NotificationType> enabledNotifications = settings.getNotificationTypes();
        if (enabledNotifications == null) {
            return Optional.empty();
        }
        return Optional.of(Collections.unmodifiableSet(enabledNotifications));
    }

    public boolean isChannelNotificationEnabled(
            final SlackChannelDefinition channel,
            final NotificationType spaceToChannelNotification) {
        Preconditions.checkNotNull(channel);
        Preconditions.checkNotNull(spaceToChannelNotification);
        Optional<Set<NotificationType>> notifications = getChannelConfiguration(channel);
        return notifications
                .map(notificationTypes -> notificationTypes.contains(spaceToChannelNotification))
                .orElse(false);
    }

    public Map<SlackChannelDefinition, SpaceToChannelSettings> getAllSpaceSettings() {
        return Collections.unmodifiableMap(channelDefinitionSpaceToChannelSettings);
    }

    public Space getSpace() {
        return space;
    }

    public Set<SlackChannelDefinition> getConfiguredChannels() {
        final Ordering<SlackChannelDefinition> ordering = Ordering
                .from(SlackChannelDefinition.ORDER_BY_NAME);

        return ImmutableSortedSet.copyOf(ordering, channelDefinitionSpaceToChannelSettings.keySet());
    }

    public Set<SlackChannelDefinition> getChannelsForNotification(
            final NotificationType notification) {
        Preconditions.checkNotNull(notification);
        Set<SlackChannelDefinition> channels = new HashSet<SlackChannelDefinition>();
        for (Map.Entry<SlackChannelDefinition, SpaceToChannelSettings> entry : channelDefinitionSpaceToChannelSettings.entrySet()) {
            Set<NotificationType> notificationTypes = entry.getValue().getNotificationTypes();
            if (notificationTypes != null && notificationTypes.contains(notification)) {
                channels.add(entry.getKey());
            }
        }
        return channels;
    }

    public static class Builder {
        // Key: channelId
        private final Map<ConversationKey, SpaceToChannelSettings.Builder> channelSettingBuilders = new HashMap<>();

        private final Space space;
        private final Function<ConversationKey, Optional<SlackChannelDefinition>> channelProvider;

        public Builder(
                final Space space,
                final Function<ConversationKey, Optional<SlackChannelDefinition>> channelProvider) {
            Preconditions.checkNotNull(space);
            Preconditions.checkNotNull(channelProvider);
            this.space = space;
            this.channelProvider = channelProvider;
        }

        /**
         * This call is very expensive, do not use this unless you absolutely must
         *
         * @return A space to channel configuration object
         */
        public SpaceToChannelConfiguration build() {
            final Map<SlackChannelDefinition, SpaceToChannelSettings> configuration = new HashMap<>();

            channelSettingBuilders.forEach((conversationKey, value) -> channelProvider.apply(conversationKey).ifPresent(channel -> {
                SpaceToChannelSettings settings = value.build();
                configuration.put(channel, settings);
            }));

            return new SpaceToChannelConfiguration(space, configuration);
        }

        /**
         * This will return an existing SpaceToChannelSettings.Builder for the channelId, or create a new one.
         *
         * @param conversationKey the channel id the SpaceToChannelSettings.Builder is for
         * @return a SpaceToChannelSettings.Builder
         */
        public SpaceToChannelSettings.Builder getSettingsBuilder(ConversationKey conversationKey) {
            SpaceToChannelSettings.Builder builder = channelSettingBuilders.get(conversationKey);
            if (builder == null) {
                builder = new SpaceToChannelSettings.Builder();
                channelSettingBuilders.put(conversationKey, builder);
            }

            return builder;
        }
    }
}
