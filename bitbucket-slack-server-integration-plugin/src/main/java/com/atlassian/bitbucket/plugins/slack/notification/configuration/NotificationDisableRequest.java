package com.atlassian.bitbucket.plugins.slack.notification.configuration;

import javax.annotation.Nonnull;

/**
 * Describes the slack notifications that needs to disabled.
 */
public class NotificationDisableRequest extends AbstractNotificationUpdateRequest {
    private NotificationDisableRequest(Builder builder) {
        super(builder);
    }

    public static class Builder extends AbstractUpdateBuilder<Builder> {

        @Nonnull
        public NotificationDisableRequest build() {
            validate(repository, "Repository ID is required for configuring a notification");
            return new NotificationDisableRequest(this);
        }

        @Override
        public Builder self() {
            return this;
        }
    }
}
