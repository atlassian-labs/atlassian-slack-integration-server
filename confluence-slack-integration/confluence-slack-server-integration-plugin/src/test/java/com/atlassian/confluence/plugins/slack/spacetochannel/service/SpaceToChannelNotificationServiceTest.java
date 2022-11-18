package com.atlassian.confluence.plugins.slack.spacetochannel.service;

import com.atlassian.confluence.plugins.slack.spacetochannel.api.notifications.AttachmentBuilder;
import com.atlassian.confluence.plugins.slack.spacetochannel.api.notifications.ConfluenceSlackEvent;
import com.atlassian.confluence.plugins.slack.spacetochannel.configuration.SlackChannelDefinition;
import com.atlassian.confluence.plugins.slack.spacetochannel.events.SpaceToChannelLinkedEvent;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugins.slack.analytics.AnalyticsContextProvider;
import com.atlassian.plugins.slack.api.client.SlackClient;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.atlassian.plugins.slack.api.descriptor.NotificationTypeService;
import com.atlassian.plugins.slack.util.AsyncExecutor;
import com.atlassian.sal.api.message.I18nResolver;
import com.atlassian.sal.api.user.UserKey;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import com.github.seratch.jslack.api.model.Conversation;
import com.github.seratch.jslack.api.model.Message;
import io.atlassian.fugue.Either;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SpaceToChannelNotificationServiceTest {
    private static final String TEAM_ID = "T";
    private static final String CHANNEL_ID = "C";

    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private SlackClientProvider slackClientProvider;
    @Mock
    private NotificationTypeService notificationTypeService;
    @Mock
    private I18nResolver i18nResolver;
    @Mock
    private AttachmentBuilder attachmentBuilder;
    @Mock
    private NotificationTypeService.ChannelNotification channelNotification;
    @Mock
    private AsyncExecutor asyncExecutor;
    @Mock
    private AnalyticsContextProvider analyticsContextProvider;

    @Mock
    private ConfluenceSlackEvent event;
    @Mock
    private SpaceToChannelLinkedEvent channelLinkedEvent;
    @Mock
    private SlackClient client;
    @Mock
    private SlackClient userClient;
    @Mock
    private ChatPostMessageRequest.ChatPostMessageRequestBuilder messageRequestBuilder;
    @Mock
    private ChatPostMessageRequest chatPostMessageRequest;
    @Mock
    private ConfluenceUser user;
    @Mock
    private Space space;
    @Mock
    private SlackChannelDefinition slackChannelDefinition;
    @Mock
    private Conversation conversation;
    @Mock
    private Message message;

    @Captor
    private ArgumentCaptor<ChatPostMessageRequest> msgCaptor;

    @InjectMocks
    private SpaceToChannelNotificationService target;

    @Test
    public void slackNotifications_shouldCallExpectedMethods() {
        when(notificationTypeService.getNotificationsForEvent(event))
                .thenReturn(Collections.singletonList(channelNotification));
        doAnswer(args -> {
            ((Runnable) args.getArgument(0)).run();
            return null;
        }).when(asyncExecutor).run(any());
        when(slackClientProvider.withTeamId(TEAM_ID)).thenReturn(Either.right(client));
        when(messageRequestBuilder.build()).thenReturn(chatPostMessageRequest);
        when(messageRequestBuilder.mrkdwn(anyBoolean())).thenReturn(messageRequestBuilder);
        when(messageRequestBuilder.channel(any())).thenReturn(messageRequestBuilder);
        when(client.postMessage(chatPostMessageRequest)).thenReturn(Either.right(message));

        when(channelNotification.getTeamId()).thenReturn(TEAM_ID);
        when(channelNotification.getChannelId()).thenReturn(CHANNEL_ID);
        when(channelNotification.getMessage()).thenReturn(messageRequestBuilder);

        when(event.getUser()).thenReturn(user);
        when(user.getKey()).thenReturn(new UserKey("some-key"));

        target.slackNotifications(event);

        verify(client).postMessage(chatPostMessageRequest);
        verify(messageRequestBuilder).mrkdwn(true);
        verify(messageRequestBuilder).channel(CHANNEL_ID);
    }


    @Test
    public void spaceToChannelLinked_shouldCallExpectedMethods() {
        when(attachmentBuilder.userLink(user)).thenReturn("<u>");
        when(attachmentBuilder.spaceLink(space)).thenReturn("<s>");
        when(i18nResolver.getText("slack.notification.channel-linked", "<u>", "<s>")).thenReturn("txt");
        when(slackClientProvider.withTeamId(TEAM_ID)).thenReturn(Either.right(client));
        when(client.withRemoteUser()).thenReturn(Either.right(userClient));
        when(userClient.selfInviteToConversation(CHANNEL_ID)).thenReturn(Either.right(conversation));
        when(client.postMessage(msgCaptor.capture())).thenReturn(Either.right(message));

        when(channelLinkedEvent.getUser()).thenReturn(user);
        when(channelLinkedEvent.getSpace()).thenReturn(space);
        when(channelLinkedEvent.getChannel()).thenReturn(slackChannelDefinition);
        when(slackChannelDefinition.getChannelId()).thenReturn(CHANNEL_ID);
        when(slackChannelDefinition.getTeamId()).thenReturn(TEAM_ID);

        target.spaceToChannelLinked(channelLinkedEvent);

        verify(client).withRemoteUser();
        verify(userClient).selfInviteToConversation(CHANNEL_ID);

        assertThat(msgCaptor.getValue().isMrkdwn(), is(true));
        assertThat(msgCaptor.getValue().getChannel(), is(CHANNEL_ID));
        assertThat(msgCaptor.getValue().getText(), is("txt"));
    }
}
