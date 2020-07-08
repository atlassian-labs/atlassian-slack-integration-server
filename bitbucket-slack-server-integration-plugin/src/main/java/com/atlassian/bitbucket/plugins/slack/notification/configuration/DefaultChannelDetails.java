package com.atlassian.bitbucket.plugins.slack.notification.configuration;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DefaultChannelDetails implements ChannelDetails {
    String teamId;
    String teamName;
    String channelId;
    String channelName;
    boolean isPrivate;
    boolean muted;
    String verbosity;
}
