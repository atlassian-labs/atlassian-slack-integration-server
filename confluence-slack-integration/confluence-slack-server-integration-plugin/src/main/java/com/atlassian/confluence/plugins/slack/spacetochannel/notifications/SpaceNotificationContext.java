package com.atlassian.confluence.plugins.slack.spacetochannel.notifications;

import com.atlassian.confluence.plugins.slack.spacetochannel.ao.AOEntityToChannelMapping;
import com.atlassian.confluence.plugins.slack.spacetochannel.ao.EntityToChannelMappingManager;
import com.atlassian.confluence.plugins.slack.spacetochannel.api.notifications.ConfluenceSlackEvent;
import com.atlassian.confluence.plugins.slack.spacetochannel.api.notifications.SpaceToChannelNotification;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.plugins.slack.api.notification.ChannelToNotify;
import com.atlassian.plugins.slack.api.notification.NotificationType;
import com.atlassian.plugins.slack.api.notification.SlackNotification;
import com.atlassian.plugins.slack.api.notification.SlackNotificationContext;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

public class SpaceNotificationContext<T extends ConfluenceSlackEvent> implements SlackNotificationContext<T> {
    public static final String KEY = "space";

    private final EntityToChannelMappingManager entityToChannelMappingManager;

    public SpaceNotificationContext(final EntityToChannelMappingManager entityToChannelMappingManager) {
        this.entityToChannelMappingManager = entityToChannelMappingManager;
    }

    @Override
    public List<ChannelToNotify> getChannels(final T event, final NotificationType notificationType) {
        final Optional<SlackNotification<Object>> notificationOption = notificationType.getNotification();
        if (!notificationOption.isPresent()) {
            return emptyList();
        }

        if (!(notificationOption.get() instanceof SpaceToChannelNotification)) {
            return emptyList();
        }

        @SuppressWarnings("unchecked") SpaceToChannelNotification<ConfluenceSlackEvent> notification =
                (SpaceToChannelNotification) notificationOption.get();
        final Optional<Space> space = notification.getSpace(event);
        if (!space.isPresent()) {
            return emptyList();
        }

        final List<AOEntityToChannelMapping> channels = entityToChannelMappingManager.getForEntityAndType(
                space.get().getKey(), notificationType);
        return channels.stream()
                .map(mapping -> new ChannelToNotify(mapping.getTeamId(), mapping.getChannelId(), null, false))
                .collect(Collectors.toList());
    }
}
