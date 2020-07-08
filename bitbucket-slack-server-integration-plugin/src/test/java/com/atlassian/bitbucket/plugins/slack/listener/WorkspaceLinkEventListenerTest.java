package com.atlassian.bitbucket.plugins.slack.listener;

import com.atlassian.bitbucket.plugins.slack.notification.configuration.NotificationConfigurationService;
import com.atlassian.bitbucket.plugins.slack.notification.renderer.SlackNotificationRenderer;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.client.SlackClient;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.atlassian.plugins.slack.event.SlackLinkedEvent;
import com.atlassian.plugins.slack.event.SlackTeamUnlinkedEvent;
import com.atlassian.plugins.slack.util.AsyncExecutor;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.atlassian.plugins.slack.test.util.CommonTestUtil.bypass;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class WorkspaceLinkEventListenerTest {
    private static final String TEAM_ID = "T";
    private static final String SLACK_USER_ID = "SU";

    @Mock
    private NotificationConfigurationService notificationConfigurationService;
    @Mock
    private SlackClientProvider slackClientProvider;
    @Mock
    private SlackNotificationRenderer slackNotificationRenderer;
    @Mock
    private AsyncExecutor asyncExecutor;

    @Mock
    private SlackLinkedEvent slackLinkedEvent;
    @Mock
    private SlackTeamUnlinkedEvent slackTeamUnlinkedEvent;
    @Mock
    private SlackLink slackLink;
    @Mock
    private SlackClient client;

    @Captor
    private ArgumentCaptor<ChatPostMessageRequest> captor;

    @InjectMocks
    private WorkspaceLinkEventListener target;

    @Test
    public void linkWasDeleted_shouldCallExpectedMethods() {
        when(slackTeamUnlinkedEvent.getTeamId()).thenReturn(TEAM_ID);

        target.linkWasDeleted(slackTeamUnlinkedEvent);

        verify(notificationConfigurationService).removeNotificationsForTeam(TEAM_ID);
    }

    @Test
    public void linkWasCreated_shouldCallExpectedMethods() {
        bypass(asyncExecutor);
        when(slackLinkedEvent.getLink()).thenReturn(slackLink);
        when(slackLink.getTeamId()).thenReturn(TEAM_ID);
        when(slackLink.getUserId()).thenReturn(SLACK_USER_ID);
        when(slackClientProvider.withLink(slackLink)).thenReturn(client);
        when(slackNotificationRenderer.getWelcomeMessage(TEAM_ID)).thenReturn("msg");

        target.linkWasCreated(slackLinkedEvent);

        verify(client).postDirectMessage(eq(SLACK_USER_ID), captor.capture());
        verify(slackNotificationRenderer).getWelcomeMessage(TEAM_ID);
        assertThat(captor.getValue().getText(), is("msg"));
    }
}
