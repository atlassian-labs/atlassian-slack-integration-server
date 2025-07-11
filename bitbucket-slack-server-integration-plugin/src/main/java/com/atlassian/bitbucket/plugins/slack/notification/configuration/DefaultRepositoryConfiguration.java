package com.atlassian.bitbucket.plugins.slack.notification.configuration;

import com.atlassian.bitbucket.repository.Repository;
import com.google.common.collect.Maps;
import jakarta.annotation.Nonnull;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

public class DefaultRepositoryConfiguration implements RepositoryConfiguration {
    private final Repository repository;
    private final Set<ChannelConfiguration> channelConfigs;

    private DefaultRepositoryConfiguration(Builder builder) {
        repository = builder.repository;
        channelConfigs = builder.channelConfigs;
    }

    @Nonnull
    @Override
    public Repository getRepository() {
        return repository;
    }

    @Nonnull
    @Override
    public Set<ChannelConfiguration> getChannelConfigurations() {
        return channelConfigs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefaultRepositoryConfiguration that = (DefaultRepositoryConfiguration) o;

        return repository.equals(that.repository);
    }

    @Override
    public int hashCode() {
        return repository.hashCode();
    }

    public static class Builder {

        private Repository repository;
        private Map<String, DefaultChannelConfiguration.Builder> channelConfigBuilders = Maps.newHashMap();
        private Set<ChannelConfiguration> channelConfigs;

        public Builder(@Nonnull Repository repository) {
            this.repository = checkNotNull(repository, "repository");
        }

        @Nonnull
        public DefaultRepositoryConfiguration build() {
            channelConfigs = channelConfigBuilders.values().stream()
                    .map(DefaultChannelConfiguration.Builder::build)
                    .sorted(Comparator.comparing(config -> config.getChannelDetails().getChannelName()))
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            return new DefaultRepositoryConfiguration(this);
        }

        @Nonnull
        public Builder channelConfiguration(@Nonnull String notificationTypeKey, @Nonnull ChannelDetails channelDetails) {
            DefaultChannelConfiguration.Builder builder = channelConfigurationBuilder(channelDetails);
            builder.notificationConfiguration(notificationTypeKey);

            return this;
        }

        private DefaultChannelConfiguration.Builder channelConfigurationBuilder(@Nonnull ChannelDetails channelDetails) {
            String channelId = channelDetails.getChannelId();
            DefaultChannelConfiguration.Builder builder = channelConfigBuilders.get(channelId);
            if (builder == null) {
                builder = new DefaultChannelConfiguration.Builder(channelDetails);
                channelConfigBuilders.put(channelId, builder);
            }
            return builder;
        }
    }
}
