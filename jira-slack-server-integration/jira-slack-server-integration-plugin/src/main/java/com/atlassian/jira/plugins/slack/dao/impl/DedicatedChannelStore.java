package com.atlassian.jira.plugins.slack.dao.impl;

import com.atlassian.jira.plugins.slack.model.DedicatedChannel;
import com.atlassian.jira.plugins.slack.storage.json.JsonPropertyStoreStorageStrategy;
import com.atlassian.jira.plugins.slack.storage.json.JsonStoreFactory;
import com.atlassian.plugins.slack.api.ConversationKey;
import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DedicatedChannelStore extends JsonPropertyStoreStorageStrategy<DedicatedChannel> {
    static final String SLACK_DEDICATED_CHANNEL_ENTITY_NAME = "slack.integration.dedicated-channels";

    @Autowired
    public DedicatedChannelStore(@Nonnull final JsonStoreFactory jsonStoreFactory) {
        super(jsonStoreFactory.getJsonStore(SLACK_DEDICATED_CHANNEL_ENTITY_NAME, DedicatedChannel.class));
    }

    @Override
    protected Class<DedicatedChannel> getEntityType() {
        return DedicatedChannel.class;
    }

    /**
     * Find all the mappings across all the issues which involve a particular channel
     *
     * @param conversationKey the channel to search for
     * @return an Iterable&lt;DedicatedChannel&gt; giving the mappings
     */
    public List<DedicatedChannel> getAllForChannel(final ConversationKey conversationKey) {
        return findByPredicate(dedicatedChannel ->
                conversationKey.getTeamId().equals(dedicatedChannel.getTeamId()) &&
                        conversationKey.getChannelId().equals(dedicatedChannel.getChannelId()));
    }

    public List<DedicatedChannel> getAllByTeamId(final String teamId) {
        return findByPredicate(dedicatedChannel -> teamId.equals(dedicatedChannel.getTeamId()));
    }
}
