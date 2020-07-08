package com.atlassian.bitbucket.plugins.slack.notification.renderer;

import lombok.Value;

@Value
public class SimpleLine {
    int number;
    String text;
}
