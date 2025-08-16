package com.atlassian.bitbucket.plugins.slack.notification.configuration;

import com.atlassian.bitbucket.repository.Repository;
import jakarta.annotation.Nonnull;

import java.util.Set;

/**
 * Describes the slack notification settings for a {@link Repository}.
 */
public interface RepositoryConfiguration {
    /**
     * Retrieves the {@link Repository repository} to which this settings belong.
     *
     * @return the repository
     */
    @Nonnull
    Repository getRepository();

    /**
     * Retrieves the {@link ChannelConfiguration configuration} for each slack channel to which notifications should be send.
     *
     * @return the channel configs
     */
    @Nonnull
    Set<ChannelConfiguration> getChannelConfigurations();
}
