package com.atlassian.confluence.plugins.slack.spacetochannel.model;

import jakarta.annotation.Nullable;

public interface ChannelContext {
    String getTeamId();

    String getChannelId();

    @Nullable
    String getThreadTs();
}
