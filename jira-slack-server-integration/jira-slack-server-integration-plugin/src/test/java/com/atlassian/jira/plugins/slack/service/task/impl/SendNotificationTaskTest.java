package com.atlassian.jira.plugins.slack.service.task.impl;

import com.atlassian.jira.plugins.slack.model.SlackNotification;
import com.atlassian.jira.plugins.slack.model.event.PluginEvent;
import com.atlassian.jira.plugins.slack.service.notification.EventRenderer;
import com.atlassian.jira.plugins.slack.service.notification.NotificationInfo;
import com.atlassian.jira.plugins.slack.service.task.TaskExecutorService;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.client.RetryLoaderHelper;
import com.atlassian.plugins.slack.api.client.RetryUser;
import com.atlassian.plugins.slack.api.client.SlackClient;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.atlassian.plugins.slack.util.ErrorResponse;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import com.github.seratch.jslack.api.model.Message;
import io.atlassian.fugue.Either;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Function;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SendNotificationTaskTest {
    @Mock
    private EventRenderer eventRenderer;
    @Mock
    private PluginEvent event;
    @Spy
    private List<NotificationInfo> notifications = new ArrayList<>();
    @Mock
    private TaskExecutorService taskExecutorService;
    @Mock
    private SlackClientProvider slackClientProvider;
    @Mock
    private RetryLoaderHelper retryLoaderHelper;

    @Mock
    private SlackNotification slackNotification;
    @Mock
    private ChatPostMessageRequest chatPostMessageRequest;
    @Mock
    private SlackLink link;
    @Mock
    private SlackClient client;
    @Mock
    private Message message;

    @Captor
    private ArgumentCaptor<Function<SlackClient, Either<ErrorResponse, Message>>> loaderCaptor;
    @Captor
    private ArgumentCaptor<RetryUser> retryUserCaptor1;
    @Captor
    private ArgumentCaptor<RetryUser> retryUserCaptor2;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private SendNotificationTask target;

    @Test
    public void call_withResponseUrl() {
        when(eventRenderer.render(event, notifications)).thenReturn(Collections.singletonList(slackNotification));
        when(taskExecutorService.submitTask(ArgumentMatchers.any()))
                .thenAnswer(args -> ((Callable<Void>) args.getArgument(0)).call());
        when(slackClientProvider.withLink(link)).thenReturn(client);
        when(slackNotification.getSlackLink()).thenReturn(link);
        when(slackNotification.getResponseUrl()).thenReturn("url");
        when(slackNotification.getMessageRequest()).thenReturn(chatPostMessageRequest);

        target.call();

        verify(client).postResponse("url", "ephemeral", chatPostMessageRequest);
    }

    @Test
    public void call_withoutResponseUrl() {
        when(eventRenderer.render(event, notifications)).thenReturn(Collections.singletonList(slackNotification));
        when(taskExecutorService.submitTask(ArgumentMatchers.any()))
                .thenAnswer(args -> ((Callable<Void>) args.getArgument(0)).call());
        when(slackClientProvider.withLink(link)).thenReturn(client);
        when(slackNotification.getSlackLink()).thenReturn(link);
        when(slackNotification.getResponseUrl()).thenReturn(null);
        when(slackNotification.getChannelId()).thenReturn("C");
        when(slackNotification.getConfigurationOwner()).thenReturn("O");
        when(slackNotification.getMessageRequest()).thenReturn(chatPostMessageRequest);
        when(client.postMessage(chatPostMessageRequest)).thenReturn(Either.right(message));

        target.call();

        verify(retryLoaderHelper).retryWithUserTokens(
                same(client), loaderCaptor.capture(), retryUserCaptor1.capture(), retryUserCaptor2.capture());

        assertThat(loaderCaptor.getValue().apply(client).toOptional(), is(Optional.of(message)));

        retryUserCaptor1.getValue().withClient(client);
        verify(client, never()).withInstallerUserToken();
        verify(client, never()).withRemoteUserTokenIfAvailable();
        verify(client, never()).withRemoteUser();

        retryUserCaptor2.getValue().withClient(client);
        verify(client).withUserTokenIfAvailable("O");
    }
}
