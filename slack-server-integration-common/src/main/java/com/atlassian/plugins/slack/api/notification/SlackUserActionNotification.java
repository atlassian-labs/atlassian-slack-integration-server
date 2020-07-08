package com.atlassian.plugins.slack.api.notification;

import com.atlassian.plugins.slack.api.events.NotificationBlockedEvent;

import java.util.Optional;

/**
 * This represents a SlackNotification that is initiated explicitly by a user action.
 *
 * @param <T> the event class that the notification processes.
 */
public interface SlackUserActionNotification<T extends BaseSlackEvent> extends SlackNotification<T> {
    /**
     * If a notification of an event is blocked, this method will be called to produce
     * an event signalling the block.
     *
     * @return an event to be published when a notification is blocked.
     */
    Optional<NotificationBlockedEvent<T>> buildNotificationBlockedEvent(T event);
}
