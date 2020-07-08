package com.atlassian.confluence.plugins.slack.spacetochannel.service;

import com.atlassian.confluence.plugins.slack.spacetochannel.configuration.SpaceToChannelConfiguration;
import com.atlassian.confluence.plugins.slack.spacetochannel.configuration.SpaceToChannelSettings;
import com.atlassian.plugins.slack.api.notification.NotificationType;

import java.util.List;
import java.util.Optional;

/**
 * Service for getting and persisting a space to channel configuration for a space.
 */
public interface SlackSpaceToChannelService {
    /**
     * This method can be used to iterate over all {@link com.atlassian.confluence.plugins.slack.spacetochannel.configuration.SpaceToChannelConfiguration}s.
     *
     * @return an object allowing the iteration of all {@link com.atlassian.confluence.plugins.slack.spacetochannel.configuration.SpaceToChannelConfiguration}s.
     */
    List<SpaceToChannelConfiguration> getAllSpaceToChannelLinks();

    /**
     * This method returns a @{link List} of all {@link com.atlassian.confluence.plugins.slack.spacetochannel.configuration.SpaceToChannelConfiguration}s.
     *
     * @return a @{link List} of all {@link com.atlassian.confluence.plugins.slack.spacetochannel.configuration.SpaceToChannelConfiguration}s.
     */
    List<SpaceToChannelConfiguration> getAllSpaceToChannelConfigurations();

    /**
     * This method can be used to retrieve a {@link com.atlassian.confluence.plugins.slack.spacetochannel.configuration.SpaceToChannelConfiguration}
     * for a specific {@link com.atlassian.confluence.spaces.Space}.
     * <p/>
     * <b>Warning: This method is very expensive, as it fills in channel information by querying HC. Do not use this method
     * unless you are sure you need to.</b>
     *
     * @param spaceKey the key of the {@link com.atlassian.confluence.spaces.Space} of interest.
     * @return the {@link com.atlassian.confluence.plugins.slack.spacetochannel.configuration.SpaceToChannelConfiguration}
     * for the {@link com.atlassian.confluence.spaces.Space} of interest.
     */
    SpaceToChannelConfiguration getSpaceToChannelConfiguration(String spaceKey);

    /**
     * Get the space to channel configuration settings for a particular space and channel.
     *
     * @param spaceKey  the space key
     * @param channelId the channel id
     * @return an SpaceToChannelSettings, if found
     */
    Optional<SpaceToChannelSettings> getSpaceToChannelSettings(String spaceKey, String channelId);

    /**
     * This method indicates whether a {@link com.atlassian.confluence.spaces.Space} has a {@link
     * com.atlassian.confluence.plugins.slack.spacetochannel.configuration.SpaceToChannelConfiguration}.
     *
     * @param spaceKey the key of the {@link com.atlassian.confluence.spaces.Space} of interest.
     * @return true if the {@link com.atlassian.confluence.spaces.Space} has a {@link com.atlassian.confluence.plugins.slack.spacetochannel.configuration.SpaceToChannelConfiguration}.
     */
    boolean hasSpaceToChannelConfiguration(String spaceKey);

    /**
     * This method can be used to enable notifications of a specific type for a given space and channel combination.
     *
     * @param spaceKey         the key of the {@link com.atlassian.confluence.spaces.Space} of interest.
     * @param channelId        the ID of the channel to send the notifications to.
     * @param notificationType the type of notification that this mapping pertains to.
     */
    void addNotificationForSpaceAndChannel(
            final String spaceKey,
            final String owner,
            final String teamId,
            final String channelId,
            final NotificationType notificationType);

    /**
     * This method can be used to disable notifications of a specific type for a given space and channel combination.
     *
     * @param spaceKey         the key of the {@link com.atlassian.confluence.spaces.Space} of interest.
     * @param channelId        the ID of the channel to stop sending the notifications to.
     * @param notificationType the type of notification that this mapping pertains to.
     */
    void removeNotificationForSpaceAndChannel(
            final String spaceKey,
            final String channelId,
            final NotificationType notificationType);

    /**
     * This method can be used to disable notifications of all types for a given space and channel combination.
     *
     * @param spaceKey  the key of the {@link com.atlassian.confluence.spaces.Space} of interest.
     * @param channelId the ID of the channel to stop sending the notifications to.
     */
    void removeNotificationsForSpaceAndChannel(
            final String spaceKey,
            final String channelId);


    /**
     * This method can be used to disable notifications of all types for a given space.
     *
     * @param spaceKey the key of the {@link com.atlassian.confluence.spaces.Space} of interest.
     * @return the number of notifications removed
     */
    int removeNotificationsForSpace(String spaceKey);

    /**
     * Used to check if a mapping exists for these parameters.
     *
     * @param entity    the entity of the mapping we want to check
     * @param channelId the channel ID of the mapping to check
     * @param type      the type of mapping we want to check
     * @return true if this mapping exists in the database
     */
    boolean hasMappingForEntityChannelAndType(final String entity,
                                              final String channelId,
                                              final NotificationType type);

    void removeNotificationsForTeam(String teamId);

    /**
     * Remove all the notification mappings for specified channel across all spaces.
     *
     * @param channelId id of the channel which mappings should be removed
     */
    void removeNotificationsForChannel(String channelId);
}
