package com.atlassian.confluence.plugins.slack.spacetochannel.model;

import javax.annotation.Nullable;

public interface ChannelContext {
    String getTeamId();

    String getChannelId();

    @Nullable
    String getThreadTs();
}
