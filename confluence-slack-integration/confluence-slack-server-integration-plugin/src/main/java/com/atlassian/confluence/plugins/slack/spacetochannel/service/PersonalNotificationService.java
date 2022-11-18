package com.atlassian.confluence.plugins.slack.spacetochannel.service;

import com.atlassian.confluence.core.SpaceContentEntityObject;
import com.atlassian.confluence.mail.notification.NotificationManager;
import com.atlassian.confluence.plugins.slack.spacetochannel.events.analytic.ConfluenceNotificationSentEvent;
import com.atlassian.confluence.plugins.slack.spacetochannel.events.analytic.ConfluenceNotificationSentEvent.Type;
import com.atlassian.confluence.plugins.slack.spacetochannel.notifications.ConfluencePersonalNotificationTypes;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugins.slack.analytics.AnalyticsContext;
import com.atlassian.plugins.slack.analytics.AnalyticsContextProvider;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.atlassian.plugins.slack.api.notification.ChannelToNotify;
import com.atlassian.plugins.slack.settings.SlackSettingService;
import com.atlassian.plugins.slack.settings.SlackUserSettingsService;
import com.atlassian.plugins.slack.user.SlackUserManager;
import com.atlassian.plugins.slack.util.AsyncExecutor;
import com.atlassian.plugins.slack.util.ErrorResponse;
import com.atlassian.sal.api.user.UserKey;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest.ChatPostMessageRequestBuilder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import static com.atlassian.confluence.plugins.slack.spacetochannel.notifications.ConfluencePersonalNotificationTypes.CREATOR_COMMENTS;
import static com.atlassian.confluence.plugins.slack.spacetochannel.notifications.ConfluencePersonalNotificationTypes.CREATOR_UPDATES;
import static com.atlassian.confluence.plugins.slack.spacetochannel.notifications.ConfluencePersonalNotificationTypes.WATCHER_COMMENTS;
import static com.atlassian.confluence.plugins.slack.spacetochannel.notifications.ConfluencePersonalNotificationTypes.WATCHER_UPDATES;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@Component
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class PersonalNotificationService {
    private final SlackUserManager slackUserManager;
    private final SlackUserSettingsService slackUserSettingsService;
    private final SlackSettingService slackSettingService;
    private final NotificationManager notificationManager;
    private final AsyncExecutor asyncExecutor;
    private final SlackClientProvider slackClientProvider;
    private final EventPublisher eventPublisher;
    private final AnalyticsContextProvider analyticsContextProvider;

    public void notifyForContent(final ConfluenceUser currentUser,
                                 final SpaceContentEntityObject content,
                                 final Supplier<ChatPostMessageRequestBuilder> message) {
        notifyFor(currentUser, content, message, CREATOR_UPDATES, WATCHER_UPDATES);
    }

    public void notifyForComment(final ConfluenceUser currentUser,
                                 final SpaceContentEntityObject content,
                                 final Supplier<ChatPostMessageRequestBuilder> message) {
        notifyFor(currentUser, content, message, CREATOR_COMMENTS, WATCHER_COMMENTS);
    }

    private void notifyFor(final ConfluenceUser currentUser,
                           final SpaceContentEntityObject container,
                           final Supplier<ChatPostMessageRequestBuilder> message,
                           final ConfluencePersonalNotificationTypes creatorKey,
                           final ConfluencePersonalNotificationTypes watcherKey) {
        if (slackSettingService.isPersonalNotificationsDisabled()) {
            return;
        }

        final Map<UserKey, ChannelToNotifyWrapper> userMap = new HashMap<>();
        final ConfluenceUser creator = container.getCreator();

        // author
        addUserChannelToMapIfUserIsMapped(userMap, currentUser, creator, creatorKey);

        // content watchers
        notificationManager.getNotificationsByContent(container).forEach(notification ->
                addUserChannelToMapIfUserIsMapped(userMap, currentUser, notification.getReceiver(), watcherKey));

        // space watchers for content type
        notificationManager.getNotificationsBySpaceAndType(container.getSpace(), container.getTypeEnum()).forEach(
                notification -> addUserChannelToMapIfUserIsMapped(
                        userMap, currentUser, notification.getReceiver(), watcherKey));

        userMap.values().forEach(channel -> {
            AnalyticsContext context = analyticsContextProvider.byTeamIdAndUserKey(channel.getChannel().getTeamId(),
                    currentUser.getKey().getStringValue());
            eventPublisher.publish(new ConfluenceNotificationSentEvent(context, channel.getNotificationType().name().toLowerCase(),
                    Type.PERSONAL));
        });
        userMap.values().forEach(channel -> sendDirectMessage(channel.getChannel(), message.get()));
    }

    private void addUserChannelToMapIfUserIsMapped(final Map<UserKey, ChannelToNotifyWrapper> userMap,
                                                   final ConfluenceUser currentUser,
                                                   final ConfluenceUser receiver,
                                                   final ConfluencePersonalNotificationTypes type) {
        final boolean isAuthorCurrentActor = Objects.equals(receiver, currentUser);
        if (!isAuthorCurrentActor) {
            final boolean isAssigneeToBeNotified = slackUserSettingsService.isPersonalNotificationTypeEnabled(receiver.getKey(), type);
            if (isAssigneeToBeNotified) {
                addUserChannelToMapIfUserIsMapped(userMap, receiver, type);
            }
        }
    }

    private void addUserChannelToMapIfUserIsMapped(final Map<UserKey, ChannelToNotifyWrapper> userMap,
                                                   final ConfluenceUser applicationUser,
                                                   final ConfluencePersonalNotificationTypes notificationType) {
        if (userMap.containsKey(applicationUser.getKey())) {
            return;
        }

        final String notificationTeamId = slackUserSettingsService.getNotificationTeamId(applicationUser.getKey());
        if (isBlank(notificationTeamId)) {
            return;
        }

        slackUserManager.getByTeamIdAndUserKey(notificationTeamId, applicationUser.getKey().getStringValue())
                .filter(user -> isNotEmpty(user.getUserToken()))
                .map(user -> new ChannelToNotify(
                        notificationTeamId,
                        user.getSlackUserId(),
                        null,
                        true))
                .ifPresent(info -> userMap.put(applicationUser.getKey(), new ChannelToNotifyWrapper(info, notificationType)));
    }

    private void sendDirectMessage(final ChannelToNotify channel,
                                   final ChatPostMessageRequestBuilder message) {
        asyncExecutor.run(() -> slackClientProvider.withTeamId(channel.getTeamId())
                .leftMap(ErrorResponse::new)
                .flatMap(client -> client.postDirectMessage(channel.getChannelId(), message.mrkdwn(true).build()))
                .left()
                .forEach(e -> {
                    log.warn("Unable to send Slack Notification. Reason: {}", e.getMessage());
                    if (log.isDebugEnabled()) {
                        log.debug("Detailed exception: ", e.getException());
                    }
                }));
    }

    @Value
    private static class ChannelToNotifyWrapper {
        ChannelToNotify channel;
        ConfluencePersonalNotificationTypes notificationType;
    }
}

