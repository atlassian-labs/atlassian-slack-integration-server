package com.atlassian.bitbucket.plugins.slack.listener;

import com.atlassian.bitbucket.plugins.slack.event.RepositoryLinkedEvent;
import com.atlassian.bitbucket.plugins.slack.notification.renderer.SlackNotificationRenderer;
import com.atlassian.plugins.slack.api.client.SlackClient;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.atlassian.plugins.slack.util.AsyncExecutor;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import com.github.seratch.jslack.api.model.Conversation;
import io.atlassian.fugue.Either;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.atlassian.plugins.slack.test.util.CommonTestUtil.bypass;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RepositoryLinkedEventListenerTest {
    private static final String TEAM_ID = "T";
    private static final String CHANNEL_ID = "C";

    @Mock
    private SlackClientProvider slackClientProvider;
    @Mock
    private SlackNotificationRenderer slackNotificationRenderer;
    @Mock
    private AsyncExecutor asyncExecutor;

    @Mock
    private RepositoryLinkedEvent repositoryLinkedEvent;
    @Mock
    private SlackClient client;
    @Mock
    private Conversation conversation;
    @Mock
    private ChatPostMessageRequest.ChatPostMessageRequestBuilder messageBuilder;
    @Mock
    private ChatPostMessageRequest message;

    @InjectMocks
    private RepositoryLinkedEventListener target;

    @Test
    void onRepositoryLinked_shouldCallExpectedMethods() {
        bypass(asyncExecutor);
        when(slackClientProvider.withTeamId(TEAM_ID)).thenReturn(Either.right(client));
        when(client.withRemoteUser()).thenReturn(Either.right(client));
        when(slackNotificationRenderer.getRepositoryLinkedMessage(repositoryLinkedEvent)).thenReturn(messageBuilder);
        when(messageBuilder.build()).thenReturn(message);
        when(client.selfInviteToConversation(CHANNEL_ID)).thenReturn(Either.right(conversation));

        when(repositoryLinkedEvent.getTeamId()).thenReturn(TEAM_ID);
        when(repositoryLinkedEvent.getChannelId()).thenReturn(CHANNEL_ID);

        target.onRepositoryLinked(repositoryLinkedEvent);

        verify(client).selfInviteToConversation(CHANNEL_ID);
        verify(client).postMessage(message);
    }
}
