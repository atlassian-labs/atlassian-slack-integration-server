package com.atlassian.bitbucket.plugins.slack.notification.configuration.dao;

import com.atlassian.bitbucket.plugins.slack.notification.configuration.NotificationDisableRequest;
import com.atlassian.bitbucket.plugins.slack.notification.configuration.NotificationEnableRequest;
import com.atlassian.bitbucket.plugins.slack.notification.configuration.NotificationSearchRequest;
import com.atlassian.bitbucket.plugins.slack.notification.configuration.RepositoryConfiguration;
import com.atlassian.bitbucket.util.Page;
import com.atlassian.bitbucket.util.PageRequest;
import com.atlassian.plugins.slack.api.ConversationKey;
import com.atlassian.plugins.slack.api.notification.ChannelToNotify;

import javax.annotation.Nonnull;
import java.util.Set;

/**
 * Data access layer for the Slack notification config.
 */
public interface NotificationConfigurationDao {
    /**
     * Enables Slack notifications to a Slack channel for a particular {@link com.atlassian.bitbucket.repository.Repository}
     * described by {@link NotificationEnableRequest request}.
     *
     * @param request describes the notifications to be enabled
     */
    void create(@Nonnull NotificationEnableRequest request);

    /**
     * Deletes Slack notification settings which match the provided {@link NotificationDisableRequest criteria}
     *
     * @param request
     */
    void delete(@Nonnull NotificationDisableRequest request);

    void removeNotificationsForTeam(@Nonnull String teamId);

    void removeNotificationsForChannel(@Nonnull ConversationKey conversationKey);

    /**
     * Retrieves a {@link Set} of slack channel ID's which match the provided {@link NotificationSearchRequest criteria}
     *
     * @param request
     * @return
     */
    Set<ChannelToNotify> getChannelsToNotify(NotificationSearchRequest request);

    /**
     * Retrieves a page of {@link com.atlassian.bitbucket.plugins.slack.notification.configuration.ao.AoNotificationConfiguration notificationConfigs} which match the provided
     * {@link NotificationSearchRequest criteria}
     *
     * @param request
     * @param pageRequest
     * @return
     */
    @Nonnull
    Page<RepositoryConfiguration> search(@Nonnull NotificationSearchRequest request, @Nonnull PageRequest pageRequest);
}
