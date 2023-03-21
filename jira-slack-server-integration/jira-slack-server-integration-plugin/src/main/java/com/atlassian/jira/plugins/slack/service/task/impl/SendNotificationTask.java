package com.atlassian.jira.plugins.slack.service.task.impl;

import com.atlassian.jira.plugins.slack.model.SlackNotification;
import com.atlassian.jira.plugins.slack.model.event.PluginEvent;
import com.atlassian.jira.plugins.slack.service.notification.EventRenderer;
import com.atlassian.jira.plugins.slack.service.notification.NotificationInfo;
import com.atlassian.plugins.slack.api.client.RetryLoaderHelper;
import com.atlassian.plugins.slack.api.client.RetryUser;
import com.atlassian.plugins.slack.api.client.SlackClient;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.atlassian.plugins.slack.util.AsyncExecutor;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostEphemeralRequest;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import com.google.common.collect.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Callable;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

/**
 * Sends the notification to the specific channel.
 */
public class SendNotificationTask implements Callable<Void> {
    private static final Logger log = LoggerFactory.getLogger(SendNotificationTask.class);

    private final EventRenderer eventRenderer;
    private final PluginEvent event;
    private final List<NotificationInfo> notifications;
    private final AsyncExecutor asyncExecutor;
    private final SlackClientProvider slackClientProvider;
    private final RetryLoaderHelper retryLoaderHelper;

    SendNotificationTask(final EventRenderer eventRenderer,
                         final PluginEvent event,
                         final List<NotificationInfo> notifications,
                         final AsyncExecutor asyncExecutor,
                         final SlackClientProvider slackClientProvider,
                         final RetryLoaderHelper retryLoaderHelper) {
        this.eventRenderer = eventRenderer;
        this.event = event;
        this.notifications = notifications;
        this.asyncExecutor = asyncExecutor;
        this.slackClientProvider = slackClientProvider;
        this.retryLoaderHelper = retryLoaderHelper;
    }

    @Override
    public Void call() {
        final List<SlackNotification> messages = eventRenderer.render(event, notifications);
        log.info("Found {} messages to send to Slack", Iterables.size(messages));
        for (final SlackNotification message : messages) {
            asyncExecutor.run(() -> {
                final SlackClient client = slackClientProvider.withLink(message.getSlackLink());

                if (message.getResponseUrl() != null) {
                    log.debug("Sending ephemeral response to Slack slash command");

                    client.postResponse(
                            message.getResponseUrl(),
                            "ephemeral",
                            message.getMessageRequest());
                } else if (message.isPersonal()) {
                    log.info("Sending personal notification to user {}", message.getChannelId());

                    client.postDirectMessage(
                            defaultIfBlank(message.getUserId(), message.getChannelId()),
                            message.getMessageRequest());
                } else if (message.isEphemeral()) {
                    // try with bot token; on error, retry with user token, if available
                    log.info("Sending ephemeral notification to Slack channel {}", message.getChannelId());

                    final ChatPostMessageRequest msg = message.getMessageRequest();
                    client.postEphemeralMessage(ChatPostEphemeralRequest.builder()
                            .text(msg.getText())
                            .attachments(msg.getAttachments())
                            .blocks(msg.getBlocks())
                            .channel(msg.getChannel())
                            .user(message.getUserId())
                            .build());
                } else {
                    // try with bot token; on error, retry with user token, if available
                    log.info("Sending notification to Slack channel {}", message.getChannelId());

                    retryLoaderHelper.retryWithUserTokens(
                            client,
                            localClient -> localClient.postMessage(message.getMessageRequest()),
                            RetryUser.botUser(),
                            RetryUser.userKey(message.getConfigurationOwner()));
                }
            });
        }
        return null;
    }
}
