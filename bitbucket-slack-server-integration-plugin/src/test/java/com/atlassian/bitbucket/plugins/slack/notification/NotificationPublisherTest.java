package com.atlassian.bitbucket.plugins.slack.notification;

import com.atlassian.bitbucket.plugins.slack.notification.configuration.NotificationConfigurationService;
import com.atlassian.bitbucket.plugins.slack.notification.configuration.NotificationSearchRequest;
import com.atlassian.bitbucket.plugins.slack.settings.BitbucketSlackSettingsService;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugins.slack.analytics.AnalyticsContextProvider;
import com.atlassian.plugins.slack.api.client.SlackClient;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.atlassian.plugins.slack.api.notification.ChannelToNotify;
import com.atlassian.plugins.slack.api.notification.Verbosity;
import com.atlassian.plugins.slack.util.AsyncExecutor;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest.ChatPostMessageRequestBuilder;
import com.github.seratch.jslack.api.model.Message;
import io.atlassian.fugue.Either;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static com.atlassian.plugins.slack.test.util.CommonTestUtil.bypass;
import static java.util.Collections.singleton;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class NotificationPublisherTest {
    private static final String TEAM_ID = "T";
    private static final String CHANNEL_ID = "C";
    private static final String THREAD_TS = "TS";
    private static final String NOTIFICATION_KEY = "NK";
    private static final int REPO_ID = 5;

    @Mock
    SlackClientProvider slackClientProvider;
    @Mock
    NotificationConfigurationService service;
    @Mock
    AsyncExecutor asyncExecutor;
    @Mock
    BitbucketSlackSettingsService bitbucketSlackSettingsService;
    @Mock
    EventPublisher eventPublisher;
    @Mock
    AnalyticsContextProvider analyticsContextProvider;

    @Mock
    Repository repository;
    @Mock
    SlackClient client;
    @Mock
    Message message;

    ChatPostMessageRequestBuilder messageBuilder;

    @InjectMocks
    NotificationPublisher renderer;

    @BeforeEach
    void setUp() {
        bypass(asyncExecutor);
        when(slackClientProvider.withTeamId(TEAM_ID)).thenReturn(Either.right(client));
        messageBuilder = ChatPostMessageRequest.builder();
    }

    @Test
    void findChannelsAndPublishNotificationsAsync_shouldPublishMessages() {
        when(service.getChannelsToNotify(any(NotificationSearchRequest.class))).thenReturn(singleton(
                new ChannelToNotify(TEAM_ID, CHANNEL_ID, THREAD_TS, false)));
        when(client.postMessage(any(ChatPostMessageRequest.class))).thenReturn(Either.right(message));

        renderer.findChannelsAndPublishNotificationsAsync(repository, NOTIFICATION_KEY, Collections::emptySet, options -> Optional.of(messageBuilder));

        verify(client).postMessage(ChatPostMessageRequest.builder().mrkdwn(true).channel(CHANNEL_ID).build());
    }

    @Test
    void findChannelsAndPublishNotificationsAsync_shouldPublishVerbosityAwareMessages() {
        when(service.getChannelsToNotify(any(NotificationSearchRequest.class))).thenReturn(singleton(
                new ChannelToNotify(TEAM_ID, CHANNEL_ID, THREAD_TS, false)));
        when(client.postMessage(any(ChatPostMessageRequest.class))).thenReturn(Either.right(message));
        when(repository.getId()).thenReturn(REPO_ID);
        when(bitbucketSlackSettingsService.getVerbosity(REPO_ID, TEAM_ID, CHANNEL_ID)).thenReturn(Verbosity.EXTENDED);

        renderer.findChannelsAndPublishNotificationsAsync(repository, NOTIFICATION_KEY, Collections::emptySet, options -> Optional.of(messageBuilder));

        verify(client).postMessage(ChatPostMessageRequest.builder().mrkdwn(true).channel(CHANNEL_ID).build());
    }

    @Test
    void sendMessageAsync_shouldPublishMessages() {
        when(client.postMessage(any(ChatPostMessageRequest.class))).thenReturn(Either.right(message));

        renderer.sendMessageAsync(TEAM_ID, CHANNEL_ID, messageBuilder);

        verify(client).postMessage(ChatPostMessageRequest.builder().mrkdwn(true).channel(CHANNEL_ID).build());
    }
}
