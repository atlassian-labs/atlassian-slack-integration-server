package com.atlassian.bitbucket.plugins.slack.notification.configuration;

import jakarta.annotation.Nonnull;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Request for searching Slack notification settings
 */
public class NotificationSearchRequest extends AbstractNotificationRequest {
    private final Optional<String> notificationTypeKey;

    private NotificationSearchRequest(Builder builder) {
        super(builder);
        notificationTypeKey = builder.notificationTypeKey;
    }

    @Nonnull
    public Optional<String> getNotificationTypeKey() {
        return notificationTypeKey;
    }

    public static class Builder extends AbstractBuilder<Builder> {
        private Optional<String> notificationTypeKey = Optional.empty();

        @Nonnull
        public NotificationSearchRequest build() {
            return new NotificationSearchRequest(this);
        }

        @Nonnull
        public Builder notificationType(@Nonnull String value) {
            notificationTypeKey = Optional.of(checkNotNull(value, "notification type"));
            return self();
        }

        @Override
        public Builder self() {
            return this;
        }
    }
}
