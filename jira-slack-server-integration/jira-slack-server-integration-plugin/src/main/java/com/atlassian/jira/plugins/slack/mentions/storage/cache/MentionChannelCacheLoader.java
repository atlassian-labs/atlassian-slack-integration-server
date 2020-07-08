package com.atlassian.jira.plugins.slack.mentions.storage.cache;

import com.atlassian.jira.plugins.slack.model.ChannelKey;
import com.atlassian.jira.plugins.slack.model.mentions.MentionChannel;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.github.seratch.jslack.api.model.Conversation;
import io.atlassian.fugue.Pair;

import javax.annotation.Nonnull;
import java.util.Optional;

public class MentionChannelCacheLoader extends SlackApiCacheLoader<ChannelKey, MentionChannel, Pair<Conversation, SlackLink>> {
    MentionChannelCacheLoader(@Nonnull final SlackClientProvider slack) {
        super(slack);
    }

    @Override
    protected MentionChannel createCacheableEntity(final ChannelKey key, final Pair<Conversation, SlackLink> load) {
        return new MentionChannel(key, load.left(), load.right().getTeamName());
    }

    /**
     * An empty userId in the key will result in the bot user being used to fetch the conversation.
     */
    @Override
    protected Optional<Pair<Conversation, SlackLink>> fetchCacheLoad(final ChannelKey key) {
        final String teamId = key.getTeamId();
        final String channelId = key.getChannelId();

        return getSlackClientProvider()
                .withTeamId(teamId)
                .flatMap(client -> client.withUserToken(key.getUserId()))
                .fold(
                        e -> Optional.empty(),
                        client -> client.getConversationsInfo(channelId)
                                .toOptional()
                                .map(conv -> Pair.pair(conv, client.getLink())));
    }
}
