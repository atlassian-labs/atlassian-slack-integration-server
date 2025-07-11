package com.atlassian.bitbucket.plugins.slack.notification.configuration;

import com.google.common.collect.Sets;
import jakarta.annotation.Nonnull;
import lombok.EqualsAndHashCode;

import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

@EqualsAndHashCode
public class AbstractNotificationUpdateRequest extends AbstractNotificationRequest {
    private final Set<String> notificationTypeKeys;
    private final Optional<String> teamId;
    private final Optional<String> channelId;

    protected AbstractNotificationUpdateRequest(AbstractUpdateBuilder builder) {
        super(builder);
        notificationTypeKeys = builder.notificationTypeKeys;
        teamId = builder.teamId;
        channelId = builder.channelId;
    }

    /**
     * Retrieves the notification types that needs to be updated.
     *
     * @return the notification types
     */
    @Nonnull
    public Set<String> getNotificationTypeKeys() {
        return notificationTypeKeys;
    }

    @Nonnull
    public Optional<String> getTeamId() {
        return teamId;
    }

    /**
     * Retrieves the slack channel ID that needs to be updated.
     *
     * @return the slack channel ID
     */
    @Nonnull
    public Optional<String> getChannelId() {
        return channelId;
    }

    public abstract static class AbstractUpdateBuilder<B extends AbstractBuilder<B>> extends AbstractBuilder<B> {

        protected Optional<String> teamId = Optional.empty();
        protected Set<String> notificationTypeKeys = Sets.newHashSet();
        protected Optional<String> channelId = Optional.empty();

        @Nonnull
        public AbstractNotificationUpdateRequest build() {
            return new AbstractNotificationUpdateRequest(this);
        }

        @Nonnull
        public B notificationType(@Nonnull String notificationTypeKey) {
            notificationTypeKeys.add(checkNotNull(notificationTypeKey, "notification type"));
            return self();
        }

        @Nonnull
        public B notificationTypes(@Nonnull Iterable<String> newNotificationTypeKeys) {
            notificationTypeKeys.addAll(Sets.newHashSet(checkNotNull(newNotificationTypeKeys, "notification types")));
            return self();
        }

        @Nonnull
        public B channelId(@Nonnull String value) {
            channelId = Optional.of(checkNotBlank(value, "channelId"));
            return self();
        }

        @Nonnull
        public B teamId(@Nonnull String value) {
            teamId = Optional.of(checkNotBlank(value, "teamId"));
            return self();
        }

        protected void validate(Optional value, String errorMessage) {
            checkState(value.isPresent(), errorMessage);
        }
    }
}
