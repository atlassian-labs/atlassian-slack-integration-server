package com.atlassian.confluence.plugins.slack.spacetochannel.configuration;

import com.atlassian.plugins.slack.api.notification.NotificationType;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents configuration settings across all notifications for all notification types.
 */
public class SpaceToChannelSettings {
    private final Set<NotificationType> notificationTypes;

    private SpaceToChannelSettings(Set<NotificationType> notificationTypes) {
        if (notificationTypes == null) {
            this.notificationTypes = Collections.emptySet();
        } else {
            this.notificationTypes = Collections.unmodifiableSet(notificationTypes);
        }
    }

    public Set<NotificationType> getNotificationTypes() {
        return Collections.unmodifiableSet(notificationTypes);
    }

    public static class Builder {
        private Set<NotificationType> notificationsTypes = new HashSet<>();

        public Builder() {
        }

        public Builder addNotificationType(NotificationType type) {
            if (type != null) {
                notificationsTypes.add(type);
            }
            return this;
        }

        public SpaceToChannelSettings build() {
            return new SpaceToChannelSettings(notificationsTypes);
        }
    }
}
