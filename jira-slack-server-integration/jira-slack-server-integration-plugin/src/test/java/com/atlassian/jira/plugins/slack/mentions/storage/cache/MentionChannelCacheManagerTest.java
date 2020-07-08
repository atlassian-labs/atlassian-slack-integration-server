package com.atlassian.jira.plugins.slack.mentions.storage.cache;

import com.atlassian.cache.CacheManager;
import com.atlassian.jira.plugins.slack.model.mentions.MentionChannel;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;

public class MentionChannelCacheManagerTest {
    @Mock
    private CacheManager cacheManager;
    @Mock
    private SlackClientProvider slackClientProvider;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private MentionChannelCacheManager target;

    @Test
    public void getEntityType() {
        Class<MentionChannel> result = target.getEntityType();
        assertThat(result, sameInstance(MentionChannel.class));
    }
}
