package com.atlassian.bitbucket.plugins.slack.model;

import com.atlassian.bitbucket.user.ApplicationUser;
import com.atlassian.plugins.slack.api.notification.ChannelToNotify;
import jakarta.annotation.Nullable;
import lombok.Value;

@Value
public class ExtendedChannelToNotify {
    ChannelToNotify channel;
    String notificationKey;
    @Nullable
    ApplicationUser applicationUser;
}
