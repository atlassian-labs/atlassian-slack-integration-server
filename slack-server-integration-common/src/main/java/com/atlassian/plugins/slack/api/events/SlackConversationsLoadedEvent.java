package com.atlassian.plugins.slack.api.events;

import com.github.seratch.jslack.api.model.Conversation;

import java.util.List;

public class SlackConversationsLoadedEvent {
    private final List<Conversation> conversations;
    private final String teamId;

    public SlackConversationsLoadedEvent(List<Conversation> conversations, String teamId) {
        this.conversations = conversations;
        this.teamId = teamId;
    }

    public List<Conversation> getConversations() {
        return conversations;
    }

    public String getTeamId() {
        return teamId;
    }
}
