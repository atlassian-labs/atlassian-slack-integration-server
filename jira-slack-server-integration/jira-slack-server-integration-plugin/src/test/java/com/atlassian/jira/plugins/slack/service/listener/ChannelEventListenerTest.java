package com.atlassian.jira.plugins.slack.service.listener;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.plugins.slack.manager.ProjectConfigurationManager;
import com.atlassian.jira.plugins.slack.model.ProjectConfiguration;
import com.atlassian.jira.webtests.ztests.admin.TestAdminMenuLinks;
import com.atlassian.plugins.slack.api.ConversationKey;
import com.atlassian.plugins.slack.api.events.SlackConversationsLoadedEvent;
import com.atlassian.plugins.slack.api.webhooks.ChannelArchiveSlackEvent;
import com.atlassian.plugins.slack.api.webhooks.ChannelDeletedSlackEvent;
import com.atlassian.plugins.slack.api.webhooks.ChannelUnarchiveSlackEvent;
import com.atlassian.plugins.slack.api.webhooks.SlackEvent;
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
    @Mock
    private SlackEvent slackEvent;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private ChannelEventListener target;

    @Test
    public void onChannelArchivedEvent() {
        when(channelArchiveSlackEvent.getChannel()).thenReturn("C");
        when(channelArchiveSlackEvent.getSlackEvent()).thenReturn(slackEvent);
        when(slackEvent.getTeamId()).thenReturn("T");

        target.onChannelArchivedEvent(channelArchiveSlackEvent);

        verify(projectConfigurationManager).muteProjectConfigurationsByChannelId(new ConversationKey("T", "C"));
    }

    @Test
    public void onChannelUnarchivedEvent() {
        when(channelUnarchiveSlackEvent.getChannel()).thenReturn("C");
        when(channelUnarchiveSlackEvent.getSlackEvent()).thenReturn(slackEvent);
        when(slackEvent.getTeamId()).thenReturn("T");

        target.onChannelUnarchivedEvent(channelUnarchiveSlackEvent);

        verify(projectConfigurationManager).unmuteProjectConfigurationsByChannelId(new ConversationKey("T", "C"));
    }

    @Test
    public void onChannelDeletedEvent() {
        when(channelDeletedSlackEvent.getChannel()).thenReturn("C");
        when(channelDeletedSlackEvent.getSlackEvent()).thenReturn(slackEvent);
        when(slackEvent.getTeamId()).thenReturn("T");

        target.onChannelDeletedEvent(channelDeletedSlackEvent);

        verify(projectConfigurationManager).deleteProjectConfigurationsByChannelId(new ConversationKey("T", "C"));
    }

    @Test
    public void onConversationsLoadedEvent() {
        when(projectConfigurationManager.getMutedProjectConfigurations())
                .thenReturn(Collections.singletonList(projectConfiguration));
        when(slackConversationsLoadedEvent.getConversations())
                .thenReturn(Arrays.asList(conversation1, conversation2));
        when(conversation1.getId()).thenReturn("C1");
        when(conversation2.getId()).thenReturn("C2");
        when(slackConversationsLoadedEvent.getTeamId()).thenReturn("T");
        when(projectConfiguration.getChannelId()).thenReturn("C1");
        when(projectConfiguration.getTeamId()).thenReturn("T");

        target.onConversationsLoadedEvent(slackConversationsLoadedEvent);

        verify(projectConfigurationManager).unmuteProjectConfigurationsByChannelId(new ConversationKey("T", "C1"));
        verify(projectConfigurationManager, never()).unmuteProjectConfigurationsByChannelId(new ConversationKey("T", "C2"));
    }
}
