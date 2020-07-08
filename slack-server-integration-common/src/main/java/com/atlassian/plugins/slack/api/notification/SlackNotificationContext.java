package com.atlassian.plugins.slack.api.notification;

import java.util.List;

public interface SlackNotificationContext<T> {
    List<ChannelToNotify> getChannels(T event, NotificationType notificationType);
}
