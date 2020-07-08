package com.atlassian.plugins.slack.api.client;

import com.atlassian.plugins.slack.api.SlackLink;
import com.github.seratch.jslack.api.model.Conversation;

import java.util.Map;
import java.util.Optional;

public class ConversationsAndLinks {
    private final Map<String, Conversation> conversations;
    private final Map<String, SlackLink> links;
    private final Map<String, SlackLink> linksByChannelId;

    public ConversationsAndLinks(final Map<String, Conversation> conversations,
                                 final Map<String, SlackLink> links,
                                 final Map<String, SlackLink> linksByChannelId) {
        this.conversations = conversations;
        this.links = links;
        this.linksByChannelId = linksByChannelId;
    }

    public Optional<Conversation> conversation(final String channelId) {
        return Optional.ofNullable(conversations.get(channelId));
    }

    public Optional<SlackLink> link(final String teamId) {
        return Optional.ofNullable(links.get(teamId));
    }

    public Optional<SlackLink> linkByChannelId(final String channelId) {
        return Optional.ofNullable(linksByChannelId.get(channelId));
    }
}
