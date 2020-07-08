package com.atlassian.jira.plugins.slack.mentions.storage.cache;

import com.atlassian.cache.CacheManager;
import com.atlassian.jira.plugins.slack.model.UserId;
import com.atlassian.jira.plugins.slack.model.mentions.MentionUser;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class MentionUserCacheManager extends AbstractIssueMentionCacheManager<UserId, MentionUser> {
    private static final long DEFAULT_TTL = 180;

    @Autowired
    public MentionUserCacheManager(final CacheManager cacheManager,
                                   final SlackClientProvider slack) {
        super(cacheManager, new MentionUserCacheLoader(slack), DEFAULT_TTL, TimeUnit.MINUTES);
    }

    @Override
    protected Class<MentionUser> getEntityType() {
        return MentionUser.class;
    }
}
