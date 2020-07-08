package com.atlassian.jira.plugins.slack.service.task.impl;

import com.atlassian.jira.plugins.slack.model.SlackNotification;
import com.atlassian.jira.plugins.slack.model.event.PluginEvent;
import com.atlassian.jira.plugins.slack.service.notification.EventRenderer;
import com.atlassian.jira.plugins.slack.service.notification.NotificationInfo;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.client.SlackClient;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DirectMessageTaskTest {
    @Mock
    private EventRenderer eventRenderer;
    @Mock
    private SlackClientProvider slackClientProvider;
    @Mock
    private PluginEvent event;
    @Mock
    private NotificationInfo notification;

    @Mock
    private SlackNotification slackNotification;
    @Mock
    private ChatPostMessageRequest chatPostMessageRequest;
    @Mock
    private SlackLink link;
    @Mock
    private SlackClient client;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private DirectMessageTask target;

    @Test
    public void call() {
        when(eventRenderer.render(event, Collections.singletonList(notification)))
                .thenReturn(Collections.singletonList(slackNotification));
        when(slackNotification.getSlackLink()).thenReturn(link);
        when(slackNotification.getMessageRequest()).thenReturn(chatPostMessageRequest);
        when(notification.getMessageAuthorId()).thenReturn("O");
        when(slackClientProvider.withLink(link)).thenReturn(client);

        target.call();

        verify(client).postDirectMessage("O", chatPostMessageRequest);
    }
}
