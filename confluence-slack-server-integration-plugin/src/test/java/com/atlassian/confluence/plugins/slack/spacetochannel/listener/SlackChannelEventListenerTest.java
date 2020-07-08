package com.atlassian.confluence.plugins.slack.spacetochannel.listener;

import com.atlassian.confluence.plugins.slack.spacetochannel.service.SlackSpaceToChannelService;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugins.slack.api.events.SlackConversationsLoadedEvent;
import com.atlassian.plugins.slack.api.webhooks.ChannelArchiveSlackEvent;
import com.atlassian.plugins.slack.api.webhooks.ChannelDeletedSlackEvent;
import com.atlassian.plugins.slack.api.webhooks.ChannelUnarchiveSlackEvent;
import com.atlassian.plugins.slack.settings.SlackSettingService;
import com.atlassian.plugins.slack.util.AsyncExecutor;
import com.github.seratch.jslack.api.model.Conversation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SlackChannelEventListenerTest {
    private static final String CHANNEL_ID = "C";

    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private SlackSettingService settingService;
    @Mock
    private SlackSpaceToChannelService spaceToChannelService;
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

    @InjectMocks
    private SlackChannelEventListener target;

    @Test
    public void onChannelArchivedEvent_shouldCallExpectedMethods() {
        when(channelArchiveSlackEvent.getChannel()).thenReturn(CHANNEL_ID);
        doAnswer(args -> {
            ((Runnable) args.getArgument(0)).run();
            return null;
        }).when(asyncExecutor).run(any());

        target.onChannelArchivedEvent(channelArchiveSlackEvent);

        verify(settingService).muteChannel(CHANNEL_ID);
    }

    @Test
    public void onChannelUnarchivedEvent_shouldCallExpectedMethods() {
        when(channelUnarchiveSlackEvent.getChannel()).thenReturn(CHANNEL_ID);
        doAnswer(args -> {
            ((Runnable) args.getArgument(0)).run();
            return null;
        }).when(asyncExecutor).run(any());

        target.onChannelUnarchivedEvent(channelUnarchiveSlackEvent);

        verify(settingService).unmuteChannel(CHANNEL_ID);
    }

    @Test
    public void onChannelDeletedEvent_shouldCallExpectedMethods() {
        when(channelDeletedSlackEvent.getChannel()).thenReturn(CHANNEL_ID);
        doAnswer(args -> {
            ((Runnable) args.getArgument(0)).run();
            return null;
        }).when(asyncExecutor).run(any());

        target.onChannelDeletedEvent(channelDeletedSlackEvent);

        verify(spaceToChannelService).removeNotificationsForChannel(CHANNEL_ID);
        verify(settingService).unmuteChannel(CHANNEL_ID);
    }

    @Test
    public void onConversationsLoadedEvent_shouldCallExpectedMethods() {
        List<Conversation> conversations = Collections.singletonList(conversation);
        List<String> conversationsStr = Arrays.asList(CHANNEL_ID, "X");
        when(conversation.getId()).thenReturn(CHANNEL_ID);
        when(settingService.getMutedChannelIds()).thenReturn(conversationsStr);
        when(slackConversationsLoadedEvent.getConversations()).thenReturn(conversations);
        doAnswer(args -> {
            ((Runnable) args.getArgument(0)).run();
            return null;
        }).when(asyncExecutor).run(any());

        target.onConversationsLoadedEvent(slackConversationsLoadedEvent);

        verify(settingService).unmuteChannel(CHANNEL_ID);
    }
}
