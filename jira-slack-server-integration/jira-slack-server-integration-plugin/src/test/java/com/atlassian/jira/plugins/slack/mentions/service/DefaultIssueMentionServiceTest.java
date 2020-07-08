package com.atlassian.jira.plugins.slack.mentions.service;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugins.slack.mentions.storage.cache.MentionChannelCacheManager;
import com.atlassian.jira.plugins.slack.mentions.storage.json.IssueMentionStore;
import com.atlassian.jira.plugins.slack.model.SlackIncomingMessage;
import com.atlassian.jira.plugins.slack.model.mentions.IssueMention;
import com.atlassian.plugins.slack.api.events.SlackUserMappedEvent;
import com.atlassian.plugins.slack.api.events.SlackUserUnmappedEvent;
import com.atlassian.plugins.slack.api.webhooks.ChannelDeletedSlackEvent;
import com.atlassian.plugins.slack.event.SlackTeamUnlinkedEvent;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import io.atlassian.fugue.Either;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultIssueMentionServiceTest {
    @Mock
    private IssueMentionStore issueMentionStore;
    @Mock
    private SlackLinkManager slackLinkManager;
    @Mock
    private MentionChannelCacheManager mentionChannelCacheManager;

    @Mock
    private ChannelDeletedSlackEvent channelDeletedSlackEvent;
    @Mock
    private SlackUserMappedEvent slackUserMappedEvent;
    @Mock
    private SlackUserUnmappedEvent slackUserUnmappedEvent;
    @Mock
    private SlackTeamUnlinkedEvent slackTeamUnlinkedEvent;
    @Mock
    private IssueMention issueMention;
    @Mock
    private IssueMention issueMention2;
    @Mock
    private SlackIncomingMessage message;
    @Mock
    private Issue issue;

    @Captor
    private ArgumentCaptor<Predicate<IssueMention>> predicateCaptor;
    @Captor
    private ArgumentCaptor<Optional<IssueMention>> issueMentionCaptor;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private DefaultIssueMentionService target;

    @Test
    public void onChannelDeletedEvent() {
        when(channelDeletedSlackEvent.getChannel()).thenReturn("C");

        target.onChannelDeletedEvent(channelDeletedSlackEvent);

        verify(mentionChannelCacheManager).deleteAll();
        verify(issueMentionStore).deleteAllByPropertyKey("C");
    }

    @Test
    public void onUserLinked() {
        target.onUserLinked(slackUserMappedEvent);

        verify(mentionChannelCacheManager).deleteAll();
    }

    @Test
    public void onUserUnlinked() {
        target.onUserUnlinked(slackUserUnmappedEvent);

        verify(mentionChannelCacheManager).deleteAll();
    }

    @Test
    public void onTeamDisconnection() {
        when(slackTeamUnlinkedEvent.getTeamId()).thenReturn("T");
        when(issueMention.getKey()).thenReturn("K");
        when(issueMentionStore.findByPredicate(predicateCaptor.capture())).thenReturn(Collections.singletonList(issueMention));

        target.onTeamDisconnection(slackTeamUnlinkedEvent);

        verify(issueMentionStore).deleteAllByPropertyKey("K");

        when(issueMention.getTeamId()).thenReturn("T");
        when(issueMention2.getTeamId()).thenReturn("T2");
        assertThat(predicateCaptor.getValue().test(issueMention), is(true));
        assertThat(predicateCaptor.getValue().test(issueMention2), is(false));
    }

    @Test
    public void deleteMessageMention() {
        target.deleteMessageMention("C", "ts");

        verify(issueMentionStore).deleteAllByPropertyKey("Cts");
    }

    @Test
    public void issueMentioned() {
        when(issue.getId()).thenReturn(3L);
        when(message.getTeamId()).thenReturn("T");
        when(message.getChannelId()).thenReturn("C");
        when(message.getTeamId()).thenReturn("T");
        when(message.getTs()).thenReturn("1552313422.123456");
        when(message.getText()).thenReturn("txt");
        when(message.getUser()).thenReturn("U");

        target.issueMentioned(issue, message);

        verify(issueMentionStore).put(eq(3L), eq("C1552313422.123456"), issueMentionCaptor.capture());

        Optional<IssueMention> issueMention = issueMentionCaptor.getValue();
        assertThat(issueMention.isPresent(), is(true));
        assertThat(issueMention.get().getTeamId(), is("T"));
        assertThat(issueMention.get().getChannelId(), is("C"));
        assertThat(issueMention.get().getMessageId(), is("1552313422.123456"));
        assertThat(issueMention.get().getMessageText(), is("txt"));
        assertThat(issueMention.get().getUserId(), is("U"));
        assertThat(issueMention.get().getIssueId(), is(3L));
        assertThat(issueMention.get().getDateTime(), is(new Date(1552313422000L)));
    }

    @Test
    public void issueMentioned_shouldUseLinksInPlaceOfText_whenHandlingLinkSharedEvent() {
        when(issue.getId()).thenReturn(3L);
        when(message.getTeamId()).thenReturn("T");
        when(message.getChannelId()).thenReturn("C");
        when(message.getTeamId()).thenReturn("T");
        when(message.getTs()).thenReturn("1552313422.123456");
        when(message.getText()).thenReturn("");
        when(message.getLinks()).thenReturn(asList("link1", "link2"));
        when(message.getUser()).thenReturn("U");

        target.issueMentioned(issue, message);

        verify(issueMentionStore).put(eq(3L), eq("C1552313422.123456"), issueMentionCaptor.capture());

        Optional<IssueMention> issueMention = issueMentionCaptor.getValue();
        assertThat(issueMention.isPresent(), is(true));
        assertThat(issueMention.get().getTeamId(), is("T"));
        assertThat(issueMention.get().getChannelId(), is("C"));
        assertThat(issueMention.get().getMessageId(), is("1552313422.123456"));
        assertThat(issueMention.get().getMessageText(), is("link1, link2"));
        assertThat(issueMention.get().getUserId(), is("U"));
        assertThat(issueMention.get().getIssueId(), is(3L));
        assertThat(issueMention.get().getDateTime(), is(new Date(1552313422000L)));
    }

    @Test
    public void getIssueMentions() {
        when(slackLinkManager.isAnyLinkDefined()).thenReturn(true);
        when(issueMentionStore.getAll(3L)).thenReturn(Collections.singletonList(issueMention));

        Either<Throwable, List<IssueMention>> result = target.getIssueMentions(3L);

        assertThat(result.isRight(), is(true));
        assertThat(result.right().get(), contains(issueMention));
    }

    @Test
    public void getIssueMentionsReturnEmptyIfNoLinkDefined() {
        when(slackLinkManager.isAnyLinkDefined()).thenReturn(false);

        Either<Throwable, List<IssueMention>> result = target.getIssueMentions(3L);

        assertThat(result.isLeft(), is(true));
    }
}
