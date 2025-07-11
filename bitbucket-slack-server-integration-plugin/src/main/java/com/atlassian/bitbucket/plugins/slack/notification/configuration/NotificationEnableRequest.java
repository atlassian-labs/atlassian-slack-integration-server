package com.atlassian.bitbucket.plugins.slack.notification.configuration;

import jakarta.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkState;

/**
 * Describes the slack notifications that needs to enabled.
 */
public class NotificationEnableRequest extends AbstractNotificationUpdateRequest {
    private NotificationEnableRequest(Builder builder) {
        super(builder);
    }

    public static class Builder extends AbstractUpdateBuilder<Builder> {

        @Nonnull
        public NotificationEnableRequest build() {
            checkState(!notificationTypeKeys.isEmpty(), "Notification type is required for configuring a notification");
            validate(repository, "Repository ID is required for configuring a notification");
            validate(channelId, "Channel ID is required for configuring a notification");
            validate(teamId, "Team ID is required for configuring a notification");

            return new NotificationEnableRequest(this);
        }

        @Override
        public Builder self() {
            return this;
        }
    }
}
