package com.atlassian.plugins.slack.api.events;

import com.github.seratch.jslack.api.model.Conversation;

import java.util.List;

public class SlackConversationsLoadedEvent {
    private final List<Conversation> conversations;

    public SlackConversationsLoadedEvent(List<Conversation> conversations) {
        this.conversations = conversations;
    }

    public List<Conversation> getConversations() {
        return conversations;
    }
}
