package com.atlassian.bitbucket.plugins.slack.notification.configuration;

import com.google.common.collect.ImmutableSet;

import javax.annotation.Nonnull;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public class DefaultChannelConfiguration implements ChannelConfiguration {
    private final Set<String> notificationConfigs;
    private final ChannelDetails channelDetails;

    private DefaultChannelConfiguration(Builder builder) {
        channelDetails = builder.channelDetails;
        notificationConfigs = builder.notificationConfigs.build();
    }

    @Nonnull
    @Override
    public Set<String> getNotificationConfigurationKeys() {
        return notificationConfigs;
    }

    @Nonnull
    @Override
    public ChannelDetails getChannelDetails() {
        return channelDetails;
    }

    @Override
    public boolean isEnabled(String notificationTypeKey) {
        return notificationConfigs.contains(notificationTypeKey);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefaultChannelConfiguration that = (DefaultChannelConfiguration) o;

        return channelDetails.equals(that.channelDetails);
    }

    @Override
    public int hashCode() {
        return channelDetails.hashCode();
    }

    public static class Builder {

        private ImmutableSet.Builder<String> notificationConfigs = ImmutableSet.builder();
        private ChannelDetails channelDetails;

        public Builder(@Nonnull ChannelDetails channelDetails) {
            this.channelDetails = checkNotNull(channelDetails, "channelDetails");
        }

        @Nonnull
        public DefaultChannelConfiguration build() {
            return new DefaultChannelConfiguration(this);
        }

        @Nonnull
        public Builder notificationConfiguration(@Nonnull String notificationTypeKey) {
            notificationConfigs.add(checkNotNull(notificationTypeKey, "notificationTypeKey"));
            return this;
        }
    }
}
