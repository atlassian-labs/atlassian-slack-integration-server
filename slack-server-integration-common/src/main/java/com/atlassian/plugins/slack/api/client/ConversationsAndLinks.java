package com.atlassian.plugins.slack.api.client;

import com.atlassian.plugins.slack.api.ConversationKey;
import com.atlassian.plugins.slack.api.SlackLink;
import com.github.seratch.jslack.api.model.Conversation;

import java.util.Map;
import java.util.Optional;

public class ConversationsAndLinks {
    private final Map<ConversationKey, Conversation> conversations;
    private final Map<String, SlackLink> links;
    private final Map<ConversationKey, SlackLink> linksByConversationKey;

    public ConversationsAndLinks(final Map<ConversationKey, Conversation> conversations,
                                 final Map<String, SlackLink> links,
                                 final Map<ConversationKey, SlackLink> linksByConversationKey) {
        this.conversations = conversations;
        this.links = links;
        this.linksByConversationKey = linksByConversationKey;
    }

    public Optional<Conversation> conversation(final ConversationKey conversationKey) {
        return Optional.ofNullable(conversations.get(conversationKey));
    }

    public Optional<SlackLink> link(final String teamId) {
        return Optional.ofNullable(links.get(teamId));
    }

    public Optional<SlackLink> linkByConversationKey(final ConversationKey conversationKey) {
        return Optional.ofNullable(linksByConversationKey.get(conversationKey));
    }
}
