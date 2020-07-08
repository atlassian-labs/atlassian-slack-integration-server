package com.atlassian.jira.plugins.slack.mentions.storage.cache;

import com.atlassian.jira.plugins.slack.model.ChannelKey;
import com.atlassian.jira.plugins.slack.model.mentions.MentionChannel;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.UserNotLinkedException;
import com.atlassian.plugins.slack.api.client.SlackClient;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.atlassian.plugins.slack.util.ErrorResponse;
import com.github.seratch.jslack.api.model.Conversation;
import io.atlassian.fugue.Either;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class MentionChannelCacheLoaderTest {
    @Mock
    private SlackClientProvider slackClientProvider;

    @Mock
    private ChannelKey channelKey;
    @Mock
    private SlackLink slackLink;
    @Mock
    private Conversation conversation;
    @Mock
    private SlackClient slackClient;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private MentionChannelCacheLoader target;

    @Test
    public void load() {
        when(slackLink.getTeamName()).thenReturn("TN");
        when(channelKey.getUserId()).thenReturn("U");
        when(channelKey.getChannelId()).thenReturn("C");
        when(channelKey.getTeamId()).thenReturn("T");
        when(slackClientProvider.withTeamId("T")).thenReturn(Either.right(slackClient));
        when(slackClient.withUserToken("U")).thenReturn(Either.right(slackClient));
        when(slackClient.getConversationsInfo("C")).thenReturn(Either.right(conversation));
        when(slackClient.getLink()).thenReturn(slackLink);

        Optional<MentionChannel> result = target.load(channelKey);

        assertThat(result.isPresent(), is(true));
        assertThat(result.get().getConversation(), sameInstance(conversation));
        assertThat(result.get().getKey(), sameInstance(channelKey));
        assertThat(result.get().getTeamName(), sameInstance("TN"));
    }

    @Test
    public void load_shouldReturnEmptyIfConversationIsNotFound() {
        when(channelKey.getUserId()).thenReturn("U");
        when(channelKey.getChannelId()).thenReturn("C");
        when(channelKey.getTeamId()).thenReturn("T");
        when(slackClientProvider.withTeamId("T")).thenReturn(Either.right(slackClient));
        when(slackClient.withUserToken("U")).thenReturn(Either.right(slackClient));
        when(slackClient.getConversationsInfo("C")).thenReturn(Either.left(new ErrorResponse(new Exception())));

        Optional<MentionChannel> result = target.load(channelKey);

        assertThat(result.isPresent(), is(false));
    }

    @Test
    public void load_shouldReturnEmptyIfUserIsNotConnected() {
        when(channelKey.getUserId()).thenReturn("U");
        when(channelKey.getChannelId()).thenReturn("C");
        when(channelKey.getTeamId()).thenReturn("T");
        when(slackClientProvider.withTeamId("T")).thenReturn(Either.right(slackClient));
        when(slackClient.withUserToken("U")).thenReturn(Either.left(new UserNotLinkedException("U")));

        Optional<MentionChannel> result = target.load(channelKey);

        assertThat(result.isPresent(), is(false));
    }
}
