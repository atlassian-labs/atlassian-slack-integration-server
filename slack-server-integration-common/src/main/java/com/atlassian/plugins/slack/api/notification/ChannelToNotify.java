package com.atlassian.plugins.slack.api.notification;

import lombok.Value;

@Value
public class ChannelToNotify {
    private String teamId;
    private String channelId;
    private String threadTs;
    private boolean isPersonal;
}
