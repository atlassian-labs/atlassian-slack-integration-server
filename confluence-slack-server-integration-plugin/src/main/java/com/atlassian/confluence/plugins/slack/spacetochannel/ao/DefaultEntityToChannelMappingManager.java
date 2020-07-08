package com.atlassian.confluence.plugins.slack.spacetochannel.ao;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugins.slack.api.notification.NotificationType;
import com.google.common.collect.ImmutableList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DefaultEntityToChannelMappingManager implements EntityToChannelMappingManager {
    private final ActiveObjects ao;

    @Autowired
    public DefaultEntityToChannelMappingManager(final ActiveObjects ao) {
        this.ao = ao;
    }

    @Override
    public List<AOEntityToChannelMapping> getAll() {
        return ImmutableList.copyOf(ao.find(AOEntityToChannelMapping.class));
    }

    @Override
    public List<AOEntityToChannelMapping> getForChannel(final String channelId) {
        return ImmutableList.copyOf(ao.find(AOEntityToChannelMapping.class, "CHANNEL_ID = ?", channelId));
    }

    @Override
    public int countForChannel(final String channelId) {
        return ao.count(AOEntityToChannelMapping.class, "CHANNEL_ID = ?", channelId);
    }

    @Override
    public List<AOEntityToChannelMapping> getForEntity(final String entityKey) {
        return ImmutableList.copyOf(ao.find(AOEntityToChannelMapping.class, "ENTITY_KEY = ?", entityKey));
    }

    @Override
    public boolean hasConfigurationForChannel(final String channelId) {
        return ao.count(AOEntityToChannelMapping.class, "CHANNEL_ID = ?", channelId) > 0;
    }

    @Override
    public boolean hasConfigurationForEntity(final String entityKey) {
        return ao.count(AOEntityToChannelMapping.class, "ENTITY_KEY = ?", entityKey) > 0;
    }

    @Override
    public boolean hasConfigurationForEntityChannelAndType(final String entity,
                                                           final String channelId,
                                                           final NotificationType type) {
        return ao.count(AOEntityToChannelMapping.class,
                "ENTITY_KEY = ? AND CHANNEL_ID = ? AND MESSAGE_TYPE_KEY = ?",
                entity, channelId, type.getKey()) > 0;
    }

    @Override
    public List<AOEntityToChannelMapping> getForEntityAndChannel(final String entityKey, final String channelId) {
        return ImmutableList.copyOf(ao.find(AOEntityToChannelMapping.class,
                "ENTITY_KEY = ? AND CHANNEL_ID = ?",
                entityKey,
                channelId));
    }

    @Override
    public List<AOEntityToChannelMapping> getForEntityAndType(final String entityKey,
                                                              final NotificationType notificationType) {
        return ImmutableList.copyOf(ao.find(AOEntityToChannelMapping.class,
                "ENTITY_KEY = ? AND MESSAGE_TYPE_KEY = ?",
                entityKey,
                notificationType.getKey()));
    }

    @Override
    public void addNotificationForEntityAndChannel(
            final String entityKey,
            final String owner,
            final String teamId,
            final String channelId,
            final NotificationType notificationType) {
        final String notificationTypeKey = notificationType.getKey();
        final AOEntityToChannelMapping entityToChannelMapping = ao.create(AOEntityToChannelMapping.class);
        entityToChannelMapping.setEntityKey(entityKey);
        entityToChannelMapping.setOwner(owner);
        entityToChannelMapping.setTeamId(teamId);
        entityToChannelMapping.setChannelId(channelId);
        entityToChannelMapping.setMessageTypeKey(notificationTypeKey);
        entityToChannelMapping.save();
    }

    @Override
    public int removeNotificationsForEntity(final String entityKey) {
        return ao.deleteWithSQL(
                AOEntityToChannelMapping.class,
                "ENTITY_KEY = ?",
                entityKey);
    }

    @Override
    public void removeNotificationsForEntityAndChannel(
            final String entityKey,
            final String channelId) {
        ao.deleteWithSQL(
                AOEntityToChannelMapping.class,
                "ENTITY_KEY = ? AND CHANNEL_ID = ?",
                entityKey,
                channelId);
    }

    @Override
    public void removeNotificationsForTeam(final String teamId) {
        ao.deleteWithSQL(
                AOEntityToChannelMapping.class,
                "TEAM_ID = ?",
                teamId);
    }

    @Override
    public void removeNotificationForEntityAndChannel(
            final String entityKey,
            final String channelId,
            final NotificationType notificationType) {
        ao.deleteWithSQL(
                AOEntityToChannelMapping.class,
                "ENTITY_KEY = ? AND CHANNEL_ID = ? AND MESSAGE_TYPE_KEY = ?",
                entityKey,
                channelId,
                notificationType.getKey());
    }

    @Override
    public int removeNotificationsForChannel(final String channelId) {
        return ao.deleteWithSQL(
                AOEntityToChannelMapping.class,
                "CHANNEL_ID = ?",
                channelId);
    }
}
