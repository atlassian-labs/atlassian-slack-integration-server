package com.atlassian.confluence.plugins.slack.spacetochannel.service;

import com.atlassian.confluence.plugins.slack.spacetochannel.api.notifications.AttachmentBuilder;
import com.atlassian.confluence.plugins.slack.spacetochannel.api.notifications.ConfluenceSlackEvent;
import com.atlassian.confluence.plugins.slack.spacetochannel.events.SpaceToChannelLinkedEvent;
import com.atlassian.confluence.plugins.slack.spacetochannel.events.analytic.ConfluenceNotificationSentEvent;
import com.atlassian.confluence.plugins.slack.spacetochannel.events.analytic.ConfluenceNotificationSentEvent.Type;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugins.slack.analytics.AnalyticsContextProvider;
import com.atlassian.plugins.slack.api.client.SlackClient;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.atlassian.plugins.slack.api.descriptor.NotificationTypeService;
import com.atlassian.plugins.slack.api.descriptor.NotificationTypeService.ChannelNotification;
import com.atlassian.plugins.slack.util.AsyncExecutor;
import com.atlassian.plugins.slack.util.AutoSubscribingEventListener;
import com.atlassian.plugins.slack.util.ErrorResponse;
import com.atlassian.sal.api.message.I18nResolver;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest.ChatPostMessageRequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SpaceToChannelNotificationService extends AutoSubscribingEventListener {
    private final static Logger log = LoggerFactory.getLogger(SpaceToChannelNotificationService.class);

    private final SlackClientProvider slackClientProvider;
    private final NotificationTypeService notificationTypeService;
    private final I18nResolver i18nResolver;
    private final AttachmentBuilder attachmentBuilder;
    private final AsyncExecutor asyncExecutor;
    private final AnalyticsContextProvider analyticsContextProvider;

    @Autowired
    public SpaceToChannelNotificationService(final SlackClientProvider slackClientProvider,
                                             final EventPublisher eventPublisher,
                                             final NotificationTypeService notificationTypeService,
                                             final I18nResolver i18nResolver,
                                             final AttachmentBuilder attachmentBuilder,
                                             final AsyncExecutor asyncExecutor,
                                             final AnalyticsContextProvider analyticsContextProvider) {
        super(eventPublisher);
        this.slackClientProvider = slackClientProvider;
        this.notificationTypeService = notificationTypeService;
        this.i18nResolver = i18nResolver;
        this.attachmentBuilder = attachmentBuilder;
        this.asyncExecutor = asyncExecutor;
        this.analyticsContextProvider = analyticsContextProvider;
    }

    @EventListener
    public void slackNotifications(final ConfluenceSlackEvent event) {
        asyncExecutor.run(() -> {
            try {
                final List<ChannelNotification> notifications = notificationTypeService
                        .getNotificationsForEvent(event);
                notifications.forEach(notification -> {
                    eventPublisher.publish(new ConfluenceNotificationSentEvent(
                            analyticsContextProvider.byTeamIdAndUserKey(notification.getTeamId(),
                            event.getUser().getKey().getStringValue()), notification.getNotificationKey(),
                            Type.REGULAR));
                });
                sendMessages(notifications);
            } catch (RuntimeException ex) {
                log.error("Failed to obtain slack notifications for event {}: {}", event, ex.getMessage());
                log.debug("Failed to obtain slack notifications for event {}", event, ex);
            }
        });
    }

    @EventListener
    public void spaceToChannelLinked(final SpaceToChannelLinkedEvent event) {
        final String text = i18nResolver.getText("slack.notification.channel-linked",
                attachmentBuilder.userLink(event.getUser()),
                attachmentBuilder.spaceLink(event.getSpace()));

        slackClientProvider.withTeamId(event.getChannel().getTeamId())
                .flatMap(SlackClient::withRemoteUser)
                .leftMap(ErrorResponse::new)
                .forEach(client -> client.selfInviteToConversation(event.getChannel().getChannelId()));
        final ChatPostMessageRequestBuilder message = ChatPostMessageRequest.builder().text(text);

        sendMessage(event.getChannel().getTeamId(), event.getChannel().getChannelId(), message);
    }

    private void sendMessages(final List<ChannelNotification> notifications) {
        for (ChannelNotification notification : notifications) {
            final String teamId = notification.getTeamId();
            final String channelId = notification.getChannelId();
            final ChatPostMessageRequestBuilder message = notification.getMessage();
            sendMessage(teamId, channelId, message);
        }
    }

    private void sendMessage(final String teamId,
                             final String channelId,
                             final ChatPostMessageRequestBuilder message) {
        slackClientProvider.withTeamId(teamId)
                .leftMap(ErrorResponse::new)
                .flatMap(client -> client.postMessage(message.mrkdwn(true).channel(channelId).build()))
                .left()
                .forEach(e -> {
                    log.warn("Unable to send Slack Notification. Reason: {}", e.getMessage());
                    if (log.isDebugEnabled()) {
                        log.debug("Detailed exception: ", e.getException());
                    }
                });
    }
}
