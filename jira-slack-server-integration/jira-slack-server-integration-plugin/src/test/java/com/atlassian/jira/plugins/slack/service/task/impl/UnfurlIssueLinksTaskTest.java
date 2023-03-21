package com.atlassian.jira.plugins.slack.service.task.impl;

import com.atlassian.jira.plugins.slack.model.SlackNotification;
import com.atlassian.jira.plugins.slack.model.event.JiraCommandEvent;
import com.atlassian.jira.plugins.slack.service.notification.EventRenderer;
import com.atlassian.jira.plugins.slack.service.notification.NotificationInfo;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.SlackUser;
import com.atlassian.plugins.slack.api.client.SlackClient;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.atlassian.plugins.slack.user.SlackUserManager;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import com.github.seratch.jslack.api.model.Attachment;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UnfurlIssueLinksTaskTest {
    @Mock
    private EventRenderer eventRenderer;
    @Mock
    private SlackClientProvider slackClientProvider;
    @Mock
    private SlackUserManager slackUserManager;

    @Mock
    private JiraCommandEvent jiraCommandEvent;
    @Mock
    private NotificationInfo notificationInfo;
    @Mock
    private SlackNotification slackNotification;
    @Mock
    private Attachment attachment;
    @Mock
    private SlackLink link;
    @Mock
    private SlackClient client;
    @Mock
    private SlackClient userClient;
    @Mock
    private SlackUser slackUser;
    @Mock
    private ChatPostMessageRequest chatPostMessageRequest;

    @Captor
    private ArgumentCaptor<Map<String, Attachment>> captor;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Test
    public void call() {
        when(eventRenderer.render(jiraCommandEvent, Collections.singletonList(notificationInfo)))
                .thenReturn(Collections.singletonList(slackNotification));
        when(slackNotification.getMessageRequest()).thenReturn(chatPostMessageRequest);
        when(chatPostMessageRequest.getAttachments()).thenReturn(Collections.singletonList(attachment));
        when(notificationInfo.getIssueUrl()).thenReturn("url");
        when(notificationInfo.getLink()).thenReturn(link);
        when(notificationInfo.getChannelId()).thenReturn("C");
        when(notificationInfo.getMessageTimestamp()).thenReturn("ts");
        when(notificationInfo.getMessageAuthorId()).thenReturn("U");
        when(slackUserManager.getBySlackUserId("U")).thenReturn(Optional.of(slackUser));
        when(slackClientProvider.withLink(link)).thenReturn(client);
        when(client.withUserTokenIfAvailable(slackUser)).thenReturn(Optional.of(userClient));

        List<Pair<JiraCommandEvent, NotificationInfo>> notificationInfos = Arrays.asList(Pair.of(
                jiraCommandEvent, notificationInfo));

        UnfurlIssueLinksTask target = new UnfurlIssueLinksTask(eventRenderer, slackClientProvider, slackUserManager,
                notificationInfos);
        target.call();

        verify(userClient).unfurl(eq("C"), eq("ts"), captor.capture());
        assertThat(captor.getValue().size(), is(1));
        assertThat(captor.getValue(), hasEntry("url", attachment));
    }
}
