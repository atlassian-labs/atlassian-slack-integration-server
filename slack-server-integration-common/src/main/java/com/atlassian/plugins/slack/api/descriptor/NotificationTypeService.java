package com.atlassian.plugins.slack.api.descriptor;

import com.atlassian.plugins.slack.api.notification.NotificationType;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest.ChatPostMessageRequestBuilder;
import lombok.Value;
import lombok.experimental.NonFinal;

import java.util.List;
import java.util.Optional;

public interface NotificationTypeService {
    List<NotificationType> getNotificationTypes();

    List<NotificationType> getVisibleNotificationTypes();

    List<NotificationType> getNotificationTypes(String context);

    List<ChannelNotification> getNotificationsForEvent(Object event);

    Optional<NotificationType> getNotificationTypeForKey(String key);

    @Value
    @NonFinal
    class ChannelNotification {
        String teamId;
        String channelId;
        String notificationKey;
        ChatPostMessageRequestBuilder message;
    }
}
