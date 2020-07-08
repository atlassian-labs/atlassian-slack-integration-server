package com.atlassian.jira.plugins.slack.model;

import com.atlassian.jira.plugins.slack.service.notification.NotificationInfo;
import com.atlassian.plugins.slack.api.SlackLink;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import lombok.AllArgsConstructor;
import lombok.Value;

/**
 * This class represents a concrete message sent to Slack. Once we have this object, no more processing is done.
 */
@Value
@AllArgsConstructor
public class SlackNotification {
    SlackLink slackLink;
    String channelId;
    String userId;
    String responseUrl;
    ChatPostMessageRequest messageRequest;
    String configurationOwner;
    boolean isPersonal;
    boolean isEphemeral;

    public SlackNotification(final NotificationInfo notificationInfo, final ChatPostMessageRequest message) {
        this(
                notificationInfo.getLink(),
                notificationInfo.getChannelId(),
                notificationInfo.getUserId(),
                notificationInfo.getResponseUrl(),
                message,
                notificationInfo.getConfigurationOwner(),
                notificationInfo.isPersonal(),
                notificationInfo.isEphemeral());
    }
}
