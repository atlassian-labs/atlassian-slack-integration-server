package com.atlassian.bitbucket.plugins.slack.notification.configuration;

import java.util.Set;

/**
 * Describes the notifications to be send to a Slack channel.
 */
public interface ChannelConfiguration {
    /**
     * Retrieves the different notifications to be send.
     *
     * @return the notification config
     */
    Set<String> getNotificationConfigurationKeys();

    /**
     * Retrieves the slack channel details, to which the notifications will be send.
     *
     * @return the channel details
     */
    ChannelDetails getChannelDetails();

    /**
     * @param notificationTypeKey
     * @return {@code true} if notification is enabled for the specified type
     */
    boolean isEnabled(String notificationTypeKey);
}
