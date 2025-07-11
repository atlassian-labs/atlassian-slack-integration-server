package com.atlassian.bitbucket.plugins.slack.model;

import com.atlassian.bitbucket.user.ApplicationUser;
import com.atlassian.plugins.slack.api.notification.Verbosity;
import jakarta.annotation.Nullable;
import lombok.Value;

@Value
public class NotificationRenderingOptions {
    Verbosity verbosity;
    boolean isPersonal;
    @Nullable
    ApplicationUser applicationUser;
}
