package com.atlassian.plugins.slack.api;

import lombok.Value;

@Value
public class ConversationKey {
    private final String teamId;
    private final String channelId;
}
