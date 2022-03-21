package com.atlassian.bitbucket.plugins.slack.model;

import com.atlassian.bitbucket.user.ApplicationUser;
import com.atlassian.plugins.slack.api.notification.Verbosity;
import lombok.Value;

import javax.annotation.Nullable;

@Value
public class NotificationRenderingOptions {
    Verbosity verbosity;
    boolean isPersonal;
    @Nullable
    ApplicationUser applicationUser;
}
