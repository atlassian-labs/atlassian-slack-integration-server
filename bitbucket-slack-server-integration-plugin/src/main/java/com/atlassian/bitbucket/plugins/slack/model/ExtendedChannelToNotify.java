package com.atlassian.bitbucket.plugins.slack.model;

import com.atlassian.bitbucket.user.ApplicationUser;
import com.atlassian.plugins.slack.api.notification.ChannelToNotify;
import lombok.Value;

import javax.annotation.Nullable;

@Value
public class ExtendedChannelToNotify {
    ChannelToNotify channel;
    String notificationKey;
    @Nullable
    ApplicationUser applicationUser;
}
