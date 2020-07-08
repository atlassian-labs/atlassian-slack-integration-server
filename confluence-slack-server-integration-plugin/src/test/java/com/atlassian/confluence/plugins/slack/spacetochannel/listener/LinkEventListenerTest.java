package com.atlassian.confluence.plugins.slack.spacetochannel.listener;

import com.atlassian.confluence.plugins.slack.spacetochannel.api.notifications.AttachmentBuilder;
import com.atlassian.confluence.plugins.slack.spacetochannel.service.SlackSpaceToChannelService;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.client.SlackClient;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.atlassian.plugins.slack.event.SlackLinkedEvent;
import com.atlassian.plugins.slack.event.SlackTeamUnlinkedEvent;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LinkEventListenerTest {
    private static final String TEAM_ID = "T";
    private static final String SLACK_USER_ID = "SU";

    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private SlackSpaceToChannelService slackSpaceToChannelService;
    @Mock
    private SlackClientProvider slackClientProvider;
    @Mock
    private AttachmentBuilder attachmentBuilder;

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
    private LinkEventListener target;

    @Test
    public void linkWasDeleted_shouldCallExpectedMethods() {
        when(slackTeamUnlinkedEvent.getTeamId()).thenReturn(TEAM_ID);

        target.linkWasDeleted(slackTeamUnlinkedEvent);

        verify(slackSpaceToChannelService).removeNotificationsForTeam(TEAM_ID);
    }

    @Test
    public void linkWasCreated_shouldCallExpectedMethods() {
        when(slackLinkedEvent.getLink()).thenReturn(slackLink);
        when(slackLink.getTeamId()).thenReturn(TEAM_ID);
        when(slackLink.getUserId()).thenReturn(SLACK_USER_ID);
        when(slackClientProvider.withLink(slackLink)).thenReturn(client);
        when(attachmentBuilder.getWelcomeMessage(TEAM_ID)).thenReturn("msg");

        target.linkWasCreated(slackLinkedEvent);

        verify(client).postDirectMessage(eq(SLACK_USER_ID), captor.capture());
        verify(attachmentBuilder).getWelcomeMessage(TEAM_ID);
        assertThat(captor.getValue().getText(), is("msg"));

    }
}
