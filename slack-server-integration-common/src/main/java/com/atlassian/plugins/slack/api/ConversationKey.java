package com.atlassian.plugins.slack.api;

import lombok.Value;

@Value
public class ConversationKey {
    String teamId;
    String channelId;

    public String toStringKey() {
        return teamId + ":" + channelId;
    }

    public static ConversationKey fromStringKey(String conversationKey) {
        String[] arr = conversationKey.split(":");
        return new ConversationKey(arr[0], arr[1]);
    }
}
