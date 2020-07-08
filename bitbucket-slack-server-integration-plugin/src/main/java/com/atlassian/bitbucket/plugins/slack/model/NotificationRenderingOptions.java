package com.atlassian.bitbucket.plugins.slack.model;

import com.atlassian.plugins.slack.api.notification.Verbosity;
import lombok.Value;

@Value
public class NotificationRenderingOptions {
    Verbosity verbosity;
    boolean isPersonal;
}
