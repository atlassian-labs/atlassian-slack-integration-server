package com.atlassian.jira.plugins.slack.service.listener;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.plugins.slack.dao.ConfigurationDAO;
import com.atlassian.jira.plugins.slack.model.event.ShowWelcomeEvent;
import com.atlassian.jira.plugins.slack.service.notification.NotificationInfo;
import com.atlassian.jira.plugins.slack.service.task.TaskBuilder;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.event.SlackLinkedEvent;
import com.atlassian.plugins.slack.event.SlackTeamUnlinkedEvent;
import com.atlassian.plugins.slack.util.AsyncExecutor;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LinkEventListenerTest {
    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private ConfigurationDAO configurationDAO;
    @Mock
    private AsyncExecutor asyncExecutor;
    @Mock
    private TaskBuilder taskBuilder;

    @Mock
    private SlackLink link;
    @Mock
    private Runnable directMessageTask;

    @Captor
    private ArgumentCaptor<NotificationInfo> notificationInfoCaptor;
    @Captor
    private ArgumentCaptor<ShowWelcomeEvent> showWelcomeEventArgumentCaptor;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private LinkEventListener target;

    @Test
    public void linkWasDeleted() {
        target.linkWasDeleted(new SlackTeamUnlinkedEvent("T"));

        verify(configurationDAO).deleteAllConfigurations("T");
    }

    @Test
    public void linkWasCreated() {
        when(link.getUserId()).thenReturn("U");
        when(link.getTeamId()).thenReturn("T");
        when(taskBuilder.newDirectMessageTask(showWelcomeEventArgumentCaptor.capture(), notificationInfoCaptor.capture()))
                .thenReturn(directMessageTask);

        target.linkWasCreated(new SlackLinkedEvent(link));

        verify(asyncExecutor).run(directMessageTask);
        assertThat(showWelcomeEventArgumentCaptor.getValue().getTeamId(), is("T"));

        NotificationInfo notifInfo = notificationInfoCaptor.getValue();
        assertThat(notifInfo.getLink(), sameInstance(link));
        assertThat(notifInfo.getChannelId(), is("U"));
        assertThat(notifInfo.getMessageAuthorId(), is("U"));
        assertThat(notifInfo.getResponseUrl(), nullValue());
    }
}
