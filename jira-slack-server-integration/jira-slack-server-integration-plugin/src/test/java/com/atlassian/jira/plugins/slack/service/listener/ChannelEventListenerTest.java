package com.atlassian.jira.plugins.slack.service.listener;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.plugins.slack.manager.ProjectConfigurationManager;
import com.atlassian.jira.plugins.slack.model.ProjectConfiguration;
import com.atlassian.plugins.slack.api.events.SlackConversationsLoadedEvent;
import com.atlassian.plugins.slack.api.webhooks.ChannelArchiveSlackEvent;
import com.atlassian.plugins.slack.api.webhooks.ChannelDeletedSlackEvent;
import com.atlassian.plugins.slack.api.webhooks.ChannelUnarchiveSlackEvent;
import com.github.seratch.jslack.api.model.Conversation;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChannelEventListenerTest {
    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private ProjectConfigurationManager projectConfigurationManager;

    @Mock
    private ProjectConfiguration projectConfiguration;
    @Mock
    private Conversation conversation1;
    @Mock
    private Conversation conversation2;
    @Mock
    private ChannelArchiveSlackEvent channelArchiveSlackEvent;
    @Mock
    private ChannelUnarchiveSlackEvent channelUnarchiveSlackEvent;
    @Mock
    private ChannelDeletedSlackEvent channelDeletedSlackEvent;
    @Mock
    private SlackConversationsLoadedEvent slackConversationsLoadedEvent;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private ChannelEventListener target;

    @Test
    public void onChannelArchivedEvent() {
        when(channelArchiveSlackEvent.getChannel()).thenReturn("C");

        target.onChannelArchivedEvent(channelArchiveSlackEvent);

        verify(projectConfigurationManager).muteProjectConfigurationsByChannelId("C");
    }

    @Test
    public void onChannelUnarchivedEvent() {
        when(channelUnarchiveSlackEvent.getChannel()).thenReturn("C");

        target.onChannelUnarchivedEvent(channelUnarchiveSlackEvent);

        verify(projectConfigurationManager).unmuteProjectConfigurationsByChannelId("C");
    }

    @Test
    public void onChannelDeletedEvent() {
        when(channelDeletedSlackEvent.getChannel()).thenReturn("C");

        target.onChannelDeletedEvent(channelDeletedSlackEvent);

        verify(projectConfigurationManager).deleteProjectConfigurationsByChannelId("C");
    }

    @Test
    public void onConversationsLoadedEvent() {
        when(projectConfigurationManager.getMutedProjectConfigurations())
                .thenReturn(Collections.singletonList(projectConfiguration));
        when(slackConversationsLoadedEvent.getConversations())
                .thenReturn(Arrays.asList(conversation1, conversation2));
        when(conversation1.getId()).thenReturn("C1");
        when(conversation2.getId()).thenReturn("C2");
        when(projectConfiguration.getChannelId()).thenReturn("C1");

        target.onConversationsLoadedEvent(slackConversationsLoadedEvent);

        verify(projectConfigurationManager).unmuteProjectConfigurationsByChannelId("C1");
        verify(projectConfigurationManager, never()).unmuteProjectConfigurationsByChannelId("C2");
    }
}
