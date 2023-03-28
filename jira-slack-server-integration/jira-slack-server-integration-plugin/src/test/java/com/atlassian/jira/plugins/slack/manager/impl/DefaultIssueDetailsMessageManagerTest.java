package com.atlassian.jira.plugins.slack.manager.impl;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugins.slack.model.DedicatedChannel;
import com.atlassian.jira.plugins.slack.model.event.ShowIssueEvent;
import com.atlassian.jira.plugins.slack.service.notification.NotificationInfo;
import com.atlassian.jira.plugins.slack.service.task.TaskBuilder;
import com.atlassian.jira.plugins.slack.service.task.impl.SendNotificationTask;
import com.atlassian.plugins.slack.analytics.AnalyticsContextProvider;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.util.AsyncExecutor;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultIssueDetailsMessageManagerTest {
    @Mock
    private AsyncExecutor asyncExecutor;
    @Mock
    private TaskBuilder taskBuilder;
    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private AnalyticsContextProvider analyticsContextProvider;

    @Mock
    private NotificationInfo notificationInfo;
    @Mock
    private Issue issue;
    @Mock
    private DedicatedChannel dedicatedChannel;
    @Mock
    private SendNotificationTask sendNotificationTask;
    @Mock
    private SlackLink slackLink;

    @Captor
    private ArgumentCaptor<ShowIssueEvent> captor;

    @InjectMocks
    private DefaultIssueDetailsMessageManager target;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Test
    public void sendIssueDetailsMessageToChannel() {
        when(taskBuilder.newSendNotificationTask(
                captor.capture(),
                same(notificationInfo),
                same(asyncExecutor))
        ).thenReturn(sendNotificationTask);
        when(notificationInfo.getLink()).thenReturn(slackLink);

        target.sendIssueDetailsMessageToChannel(notificationInfo, issue, dedicatedChannel);

        verify(asyncExecutor).run(sendNotificationTask);

        final ShowIssueEvent event = captor.getValue();
        assertThat(event.getDedicatedChannel(), is(Optional.of(dedicatedChannel)));
        assertThat(event.getIssue(), sameInstance(issue));
        assertThat(event.getCommandType(), is(ShowIssueEvent.COMMAND));
    }
}
