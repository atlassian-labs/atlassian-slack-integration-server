package com.atlassian.jira.plugins.slack.manager.impl;

import com.atlassian.jira.plugins.slack.model.event.DedicatedChannelLinkedEvent;
import com.atlassian.jira.plugins.slack.model.event.DedicatedChannelUnlinkedEvent;
import com.atlassian.jira.plugins.slack.service.notification.NotificationInfo;
import com.atlassian.jira.plugins.slack.service.task.TaskBuilder;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.plugins.slack.util.AsyncExecutor;
import io.atlassian.fugue.Either;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DedicatedChannelEventListenerTest {
    @Mock
    private AsyncExecutor asyncExecutor;
    @Mock
    private TaskBuilder taskBuilder;
    @Mock
    private SlackLinkManager slackLinkManager;

    @Mock
    private DedicatedChannelLinkedEvent dedicatedChannelLinkedEvent;
    @Mock
    private DedicatedChannelUnlinkedEvent dedicatedChannelUnlinkedEvent;
    @Mock
    private SlackLink link;
    @Mock
    private Runnable sendNotificationTask;

    @Captor
    private ArgumentCaptor<NotificationInfo> notificationInfo;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private DedicatedChannelEventListener target;

    @Test
    public void dedicatedChannelLinkedEventListener() {
        when(dedicatedChannelLinkedEvent.getTeamId()).thenReturn("T");
        when(dedicatedChannelLinkedEvent.getChannelId()).thenReturn("C");
        when(dedicatedChannelLinkedEvent.getOwner()).thenReturn("O");
        when(slackLinkManager.getLinkByTeamId("T")).thenReturn(Either.right(link));
        when(taskBuilder.newSendNotificationTask(same(dedicatedChannelLinkedEvent), notificationInfo.capture(), same(asyncExecutor)))
                .thenReturn(sendNotificationTask);

        target.dedicatedChannelLinkedEventListener(dedicatedChannelLinkedEvent);

        verify(asyncExecutor).run(sendNotificationTask);

        NotificationInfo info = notificationInfo.getValue();
        assertThat(info.getLink(), sameInstance(link));
        assertThat(info.getChannelId(), is("C"));
        assertThat(info.getConfigurationOwner(), is("O"));
    }

    @Test
    public void dedicatedChannelUnlinkedEventListener() {
        when(dedicatedChannelUnlinkedEvent.getTeamId()).thenReturn("T");
        when(dedicatedChannelUnlinkedEvent.getChannelId()).thenReturn("C");
        when(dedicatedChannelUnlinkedEvent.getOwner()).thenReturn("O");
        when(slackLinkManager.getLinkByTeamId("T")).thenReturn(Either.right(link));
        when(taskBuilder.newSendNotificationTask(same(dedicatedChannelUnlinkedEvent), notificationInfo.capture(), same(asyncExecutor)))
                .thenReturn(sendNotificationTask);

        target.dedicatedChannelUnlinkedEventListener(dedicatedChannelUnlinkedEvent);

        verify(asyncExecutor).run(sendNotificationTask);

        NotificationInfo info = notificationInfo.getValue();
        assertThat(info.getLink(), sameInstance(link));
        assertThat(info.getChannelId(), is("C"));
        assertThat(info.getConfigurationOwner(), is("O"));
    }
}
