package com.atlassian.bitbucket.plugins.slack.listener;

import com.atlassian.bitbucket.plugins.slack.notification.configuration.NotificationConfigurationService;
import com.atlassian.plugins.slack.api.ConversationKey;
import com.atlassian.plugins.slack.api.events.SlackConversationsLoadedEvent;
import com.atlassian.plugins.slack.api.webhooks.ChannelArchiveSlackEvent;
import com.atlassian.plugins.slack.api.webhooks.ChannelDeletedSlackEvent;
import com.atlassian.plugins.slack.api.webhooks.ChannelUnarchiveSlackEvent;
import com.atlassian.plugins.slack.api.webhooks.SlackEvent;
import com.atlassian.plugins.slack.settings.SlackSettingService;
import com.atlassian.plugins.slack.test.util.CommonTestUtil;
import com.atlassian.plugins.slack.util.AsyncExecutor;
import com.github.seratch.jslack.api.model.Conversation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SlackChannelEventListenerTest {
    private static final String TEAM_ID = "T";
    private static final String CHANNEL_ID = "C";

    @Mock
    private SlackSettingService settingService;
    @Mock
    private NotificationConfigurationService notificationService;
    @Mock
    private ChannelArchiveSlackEvent channelArchiveSlackEvent;
    @Mock
    private ChannelUnarchiveSlackEvent channelUnarchiveSlackEvent;
    @Mock
    private ChannelDeletedSlackEvent channelDeletedSlackEvent;
    @Mock
    private SlackConversationsLoadedEvent slackConversationsLoadedEvent;
    @Mock
    private Conversation conversation;
    @Mock
    private AsyncExecutor asyncExecutor;
    @Mock
    private SlackEvent slackEvent;

    @InjectMocks
    private SlackChannelEventListener target;

    @BeforeEach
    public void setUp() {
        CommonTestUtil.bypass(asyncExecutor);
    }

    @Test
    public void onChannelArchivedEvent_shouldCallExpectedMethods() {
        when(channelArchiveSlackEvent.getChannel()).thenReturn(CHANNEL_ID);
        when(channelArchiveSlackEvent.getSlackEvent()).thenReturn(slackEvent);
        when(slackEvent.getTeamId()).thenReturn(TEAM_ID);

        target.onChannelArchivedEvent(channelArchiveSlackEvent);

        verify(settingService).muteChannel(new ConversationKey(TEAM_ID, CHANNEL_ID));
    }

    @Test
    public void onChannelUnarchivedEvent_shouldCallExpectedMethods() {
        when(channelUnarchiveSlackEvent.getChannel()).thenReturn(CHANNEL_ID);
        when(channelUnarchiveSlackEvent.getSlackEvent()).thenReturn(slackEvent);
        when(slackEvent.getTeamId()).thenReturn(TEAM_ID);

        target.onChannelUnarchivedEvent(channelUnarchiveSlackEvent);

        verify(settingService).unmuteChannel(new ConversationKey(TEAM_ID, CHANNEL_ID));
    }

    @Test
    public void onChannelDeletedEvent_shouldCallExpectedMethods() {
        when(channelDeletedSlackEvent.getChannel()).thenReturn(CHANNEL_ID);
        when(channelDeletedSlackEvent.getSlackEvent()).thenReturn(slackEvent);
        when(slackEvent.getTeamId()).thenReturn(TEAM_ID);

        target.onChannelDeletedEvent(channelDeletedSlackEvent);

        verify(notificationService).removeNotificationsForChannel(CHANNEL_ID);
        verify(settingService).unmuteChannel(new ConversationKey(TEAM_ID, CHANNEL_ID));
    }

    @Test
    public void onConversationsLoadedEvent_shouldCallExpectedMethods() {
        List<Conversation> conversations = Collections.singletonList(conversation);
        List<ConversationKey> mutedConversations = Arrays.asList(new ConversationKey(TEAM_ID, CHANNEL_ID));
        when(conversation.getId()).thenReturn(CHANNEL_ID);
        when(settingService.getMutedChannelIds()).thenReturn(mutedConversations);
        when(slackConversationsLoadedEvent.getTeamId()).thenReturn(TEAM_ID);
        when(slackConversationsLoadedEvent.getConversations()).thenReturn(conversations);

        target.onConversationsLoadedEvent(slackConversationsLoadedEvent);

        verify(settingService).unmuteChannel(new ConversationKey(TEAM_ID, CHANNEL_ID));
    }
}
