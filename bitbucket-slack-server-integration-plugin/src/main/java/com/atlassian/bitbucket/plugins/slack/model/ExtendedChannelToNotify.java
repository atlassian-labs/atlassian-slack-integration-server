package com.atlassian.bitbucket.plugins.slack.model;

import com.atlassian.plugins.slack.api.notification.ChannelToNotify;
import lombok.Value;

@Value
public class ExtendedChannelToNotify {
    ChannelToNotify channel;
    String notificationKey;
}
