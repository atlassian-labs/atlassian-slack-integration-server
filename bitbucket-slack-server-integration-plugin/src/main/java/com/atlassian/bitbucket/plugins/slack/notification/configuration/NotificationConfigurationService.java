package com.atlassian.bitbucket.plugins.slack.notification.configuration;

import com.atlassian.bitbucket.util.Page;
import com.atlassian.bitbucket.util.PageRequest;
import com.atlassian.plugins.slack.api.ConversationKey;
import com.atlassian.plugins.slack.api.notification.ChannelToNotify;

import javax.annotation.Nonnull;
import java.util.Set;

/**
 * Describes a service for configuring slack notifications.
 */
public interface NotificationConfigurationService {
    /**
     * Disables the Slack notification settings which match the provided
     * {@link NotificationDisableRequest request}
     *
     * @param request
     */
    void disable(@Nonnull NotificationDisableRequest request);

    /**
     * Enables Slack notifications as described in the {@link NotificationEnableRequest request}
     *
     * @param request
     */
    void enable(@Nonnull NotificationEnableRequest request);

    void removeNotificationsForTeam(@Nonnull String teamId);

    void removeNotificationsForChannel(@Nonnull ConversationKey conversationKey);

    /**
     * Retrieves a {@link Set} of slack channel ID's which match the provided {@link NotificationSearchRequest criteria}
     *
     * @param request
     * @return
     */
    @Nonnull
    Set<ChannelToNotify> getChannelsToNotify(@Nonnull NotificationSearchRequest request);

    /**
     * Retrieves a page of {@link RepositoryConfiguration repositoryConfigs} which match the provided
     * {@link NotificationSearchRequest criteria}
     *
     * @param request
     * @param pageRequest
     * @return
     */
    @Nonnull
    Page<RepositoryConfiguration> search(@Nonnull NotificationSearchRequest request, @Nonnull PageRequest pageRequest);
}
