package com.atlassian.confluence.plugins.slack.spacetochannel.ao;

import com.atlassian.activeobjects.tx.Transactional;
import com.atlassian.plugins.slack.api.notification.NotificationType;

import java.util.List;

@Transactional
public interface EntityToChannelMappingManager {

    /**
     * This method can be used to iterate over all {@link AOEntityToChannelMapping}s.
     *
     * @return an object allowing the iteration of all {@link AOEntityToChannelMapping}s.
     */
    List<AOEntityToChannelMapping> getAll();

    /**
     * This method can be used to iterate over all {@link AOEntityToChannelMapping}s for a specific channel.
     *
     * @param channelId the ID of the channel.
     * @return an object allowing the iteration of all {@link AOEntityToChannelMapping}s for the channel of interest.
     */
    List<AOEntityToChannelMapping> getForChannel(String channelId);

    /**
     * This method returns the number of mappings for a specific channel.
     *
     * @param channelId the ID of the channel.
     * @return the number of mappings to the channel of interest.
     */
    int countForChannel(String channelId);

    /**
     * This method can be used to iterate over all {@link AOEntityToChannelMapping}s for a specific entity.
     *
     * @param entityKey a key identifying the entity that the mappings pertain to.
     * @return an object allowing the iteration of all {@link AOEntityToChannelMapping}s for the entity of interest.
     */
    List<AOEntityToChannelMapping> getForEntity(String entityKey);

    /**
     * This method can be used to iterate over all {@link AOEntityToChannelMapping}s for a specific entity and channel id.
     *
     * @param entityKey a key identifying the entity that the configuration pertains to.
     * @param channelId the ID of the channel.
     * @return an object allowing the iteration of all {@link AOEntityToChannelMapping}s for the entity and channel of interest.
     */
    List<AOEntityToChannelMapping> getForEntityAndChannel(String entityKey, String channelId);

    /**
     * This method can be used to iterate over all {@link AOEntityToChannelMapping}s for a specific entity and notification
     * type.
     *
     * @param entityKey        a key identifying the entity that the configuration pertains to.
     * @param notificationType the type of notification that the configuration pertains to
     * @return an object allowing the iteration of all {@link AOEntityToChannelMapping}s for the entity and notification
     * type of interest.
     */
    List<AOEntityToChannelMapping> getForEntityAndType(String entityKey, NotificationType notificationType);

    /**
     * This method indicates whether a configuration exists for a specific channel.
     *
     * @param channelId the ID of the channel.
     * @return true if a configuration exists, false otherwise.
     */
    boolean hasConfigurationForChannel(String channelId);

    /**
     * This method indicates whether a configuration exists for a specific entity.
     *
     * @param entityKey a key identifying the entity that the configuration pertains to.
     * @return true if a configuration exists, false otherwise.
     */
    boolean hasConfigurationForEntity(String entityKey);

    /**
     * This method can be used to enable notifications of a specific type for a given entity and channel combination.
     *
     * @param entityKey        a key identifying the entity that this mapping pertains to.
     * @param channelId        the ID of the channel.
     * @param notificationType the type of notification that this mapping pertains to.
     */
    void addNotificationForEntityAndChannel(
            String entityKey,
            String owner,
            String teamId,
            String channelId,
            NotificationType notificationType);

    void removeNotificationsForTeam(String teamId);

    /**
     * This method can be used to disable notifications of a specific type for a given entity and channel combination.
     *
     * @param entityKey        a key identifying the entity that this mapping pertains to.
     * @param channelId        the ID of the channel.
     * @param notificationType the type of notification that this mapping pertains to.
     */
    void removeNotificationForEntityAndChannel(
            String entityKey,
            String channelId,
            NotificationType notificationType);

    /**
     * This method can be used to disable notifications of all types for a given entity and channel combination.
     *
     * @param entityKey a key identifying the entity that this mapping pertains to.
     * @param channelId the ID of the channel.
     */
    void removeNotificationsForEntityAndChannel(String entityKey, String channelId);

    /**
     * This method can be used to disable notifications of all types for a given entity.
     *
     * @param entityKey a key identifying the entity that this mapping pertains to.
     * @return the number of notifications removed.
     */
    int removeNotificationsForEntity(String entityKey);

    /**
     * Remove all the notifications for specified channel in all spaces.
     *
     * @param channelId id of the channel which mappings should be removed
     * @return number of notifications removed
     */
    int removeNotificationsForChannel(String channelId);

    /**
     * This method is used to check if a mapping for this combination of entity, channel and notification type already
     * exists.
     *
     * @param entity    a key identifying the entity that this mapping pertains to.
     * @param channelId the ID of the channel.
     * @param type      the notification type of this mapping
     * @return true if this mapping exists in the database
     */
    boolean hasConfigurationForEntityChannelAndType(String entity, String channelId, NotificationType type);
}
