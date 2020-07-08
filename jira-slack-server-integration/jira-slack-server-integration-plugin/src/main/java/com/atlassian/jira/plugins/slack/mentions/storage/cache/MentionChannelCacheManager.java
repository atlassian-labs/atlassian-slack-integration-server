package com.atlassian.jira.plugins.slack.mentions.storage.cache;

import com.atlassian.cache.CacheManager;
import com.atlassian.jira.plugins.slack.model.ChannelKey;
import com.atlassian.jira.plugins.slack.model.mentions.MentionChannel;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class MentionChannelCacheManager extends AbstractIssueMentionCacheManager<ChannelKey, MentionChannel> {
    private static final long DEFAULT_TTL = 30;

    @Autowired
    public MentionChannelCacheManager(final CacheManager cacheManager,
                                      final SlackClientProvider slack) {
        super(
                cacheManager,
                new MentionChannelCacheLoader(slack),
                DEFAULT_TTL,
                TimeUnit.MINUTES);
    }

    @Override
    protected Class<MentionChannel> getEntityType() {
        return MentionChannel.class;
    }
}
