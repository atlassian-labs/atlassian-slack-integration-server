package com.atlassian.jira.plugins.slack.service.listener;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.plugins.slack.dao.ConfigurationDAO;
import com.atlassian.jira.plugins.slack.dao.DedicatedChannelDAO;
import com.atlassian.jira.plugins.slack.model.DedicatedChannel;
import com.atlassian.jira.plugins.slack.model.ProjectConfiguration;
import com.atlassian.jira.plugins.slack.model.event.ShowBotAddedHelpEvent;
import com.atlassian.jira.plugins.slack.service.notification.NotificationInfo;
import com.atlassian.jira.plugins.slack.service.task.TaskBuilder;
import com.atlassian.jira.plugins.slack.service.task.impl.SendNotificationTask;
import com.atlassian.plugins.slack.api.ConversationKey;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.webhooks.MemberJoinedChannelSlackEvent;
import com.atlassian.plugins.slack.api.webhooks.SlackEvent;
import com.atlassian.plugins.slack.util.AsyncExecutor;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MemberJoinedEventListenerTest {
    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private ConfigurationDAO configurationDAO;
    @Mock
    private DedicatedChannelDAO dedicatedChannelDAO;
    @Mock
    private AsyncExecutor asyncExecutor;
    @Mock
    private TaskBuilder taskBuilder;

    @Mock
    private MemberJoinedChannelSlackEvent memberJoinedChannelSlackEvent;
    @Mock
    private SlackEvent slackEvent;
    @Mock
    private SlackLink link;
    @Mock
    private ProjectConfiguration projectConfiguration;
    @Mock
    private DedicatedChannel dedicatedChannel;
    @Mock
    private SendNotificationTask sendNotificationTask;

    @Captor
    private ArgumentCaptor<NotificationInfo> notificationInfoCaptor;
    @Captor
    private ArgumentCaptor<ShowBotAddedHelpEvent> eventCaptor;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private MemberJoinedEventListener target;

    @Test
    public void memberJoined_shouldPostIntroNotification() {
        when(memberJoinedChannelSlackEvent.getSlackEvent()).thenReturn(slackEvent);
        when(memberJoinedChannelSlackEvent.getUser()).thenReturn("B");
        when(memberJoinedChannelSlackEvent.getChannel()).thenReturn("C");
        when(slackEvent.getSlackLink()).thenReturn(link);
        when(link.getBotUserId()).thenReturn("B");
        when(configurationDAO.findByChannel(new ConversationKey("T", "C"))).thenReturn(Collections.emptyList());
        when(dedicatedChannelDAO.findMappingsForChannel(new ConversationKey("T", "C"))).thenReturn(Collections.emptyList());

        when(taskBuilder.newSendNotificationTask(eventCaptor.capture(), notificationInfoCaptor.capture(), same(asyncExecutor)))
                .thenReturn(sendNotificationTask);

        target.memberJoined(memberJoinedChannelSlackEvent);

        verify(asyncExecutor).run(sendNotificationTask);
        assertThat(eventCaptor.getValue().getSlackLink(), sameInstance(link));
        assertThat(eventCaptor.getValue().getChannelId(), is("C"));

        NotificationInfo notifInfo = notificationInfoCaptor.getValue();
        assertThat(notifInfo.getLink(), sameInstance(link));
        assertThat(notifInfo.getChannelId(), is("C"));
    }

    @Test
    public void memberJoined_shouldDoNothingIfUserIsNotBot() {
        when(memberJoinedChannelSlackEvent.getSlackEvent()).thenReturn(slackEvent);
        when(memberJoinedChannelSlackEvent.getUser()).thenReturn("U");
        when(slackEvent.getSlackLink()).thenReturn(link);
        when(link.getBotUserId()).thenReturn("B");

        target.memberJoined(memberJoinedChannelSlackEvent);

        verify(configurationDAO, never()).findByChannel(any());
        verify(asyncExecutor, never()).run(sendNotificationTask);
    }

    @Test
    public void memberJoined_shouldDoNothingIfThereIsConfiguration() {
        when(memberJoinedChannelSlackEvent.getSlackEvent()).thenReturn(slackEvent);
        when(memberJoinedChannelSlackEvent.getUser()).thenReturn("B");
        when(memberJoinedChannelSlackEvent.getChannel()).thenReturn("C");
        when(slackEvent.getSlackLink()).thenReturn(link);
        when(link.getBotUserId()).thenReturn("B");
        when(configurationDAO.findByChannel(new ConversationKey("T", "C"))).thenReturn(Collections.singletonList(projectConfiguration));

        target.memberJoined(memberJoinedChannelSlackEvent);

        verify(asyncExecutor, never()).run(sendNotificationTask);
    }

    @Test
    public void memberJoined_shouldDoNothingIfChannelIsDedicated() {
        when(memberJoinedChannelSlackEvent.getSlackEvent()).thenReturn(slackEvent);
        when(memberJoinedChannelSlackEvent.getUser()).thenReturn("B");
        when(memberJoinedChannelSlackEvent.getChannel()).thenReturn("C");
        when(slackEvent.getSlackLink()).thenReturn(link);
        when(link.getBotUserId()).thenReturn("B");
        when(configurationDAO.findByChannel(new ConversationKey("T", "C"))).thenReturn(Collections.emptyList());
        when(dedicatedChannelDAO.findMappingsForChannel(new ConversationKey("T", "C"))).thenReturn(Collections.singletonList(dedicatedChannel));

        target.memberJoined(memberJoinedChannelSlackEvent);

        verify(asyncExecutor, never()).run(sendNotificationTask);
    }
}
