package com.atlassian.bitbucket.plugins.slack.notification.configuration;

import jakarta.annotation.Nonnull;

/**
 * Describes the Slack channel.
 */
public interface ChannelDetails {
    @Nonnull
    String getTeamId();

    /**
     * Retrieves the slack channel ID.
     *
     * @return the slack channel ID
     */
    @Nonnull
    String getChannelId();

    /**
     * Retrieves the slack channel name.
     *
     * @return the channel name
     */
    @Nonnull
    String getChannelName();

    /**
     * Retrieves the {@link ChannelDetails.State state} of slack channel.
     */
    @Nonnull
    boolean isMuted();

    /**
     * @return verbosity of the channel mapping
     */
    @Nonnull
    String getVerbosity();

}
