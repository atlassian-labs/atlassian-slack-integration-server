package com.atlassian.jira.plugins.slack.service.listener.impl;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.event.issue.DelegatingJiraIssueEvent;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.event.issue.IssueEventBundle;
import com.atlassian.jira.event.operation.SpanningOperation;
import com.atlassian.jira.event.type.EventType;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugins.slack.manager.DedicatedChannelManager;
import com.atlassian.jira.plugins.slack.model.EventMatcherType;
import com.atlassian.jira.plugins.slack.model.event.DefaultJiraIssueEvent;
import com.atlassian.jira.plugins.slack.model.event.PluginEvent;
import com.atlassian.jira.plugins.slack.service.listener.IssueEventToEventMatcherTypeConverter;
import com.atlassian.jira.plugins.slack.service.notification.IssueEventProcessorService;
import com.atlassian.jira.plugins.slack.service.notification.NotificationInfo;
import com.atlassian.jira.plugins.slack.service.notification.PersonalNotificationManager;
import com.atlassian.jira.plugins.slack.service.task.TaskBuilder;
import com.atlassian.jira.plugins.slack.service.task.TaskExecutorService;
import com.atlassian.jira.plugins.slack.service.task.impl.SendNotificationTask;
import com.atlassian.jira.plugins.slack.settings.JiraSettingsService;
import com.atlassian.jira.plugins.slack.util.changelog.ChangeLogExtractor;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugins.slack.analytics.AnalyticsContextProvider;
import com.atlassian.plugins.slack.api.notification.Verbosity;
import com.atlassian.plugins.slack.test.util.CommonTestUtil;
import com.atlassian.plugins.slack.util.AsyncExecutor;
import com.google.common.collect.ImmutableMap;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultJiraSlackEventListenerTest {
    @Mock
    private TaskExecutorService taskExecutorService;
    @Mock
    private TaskBuilder taskBuilder;
    @Mock
    private IssueEventToEventMatcherTypeConverter issueEventToEventMatcherTypeConverter;
    @Mock
    private DedicatedChannelManager dedicatedChannelManager;
    @Mock
    private IssueEventProcessorService issueEventProcessorService;
    @Mock
    private JiraSettingsService jiraSettingsService;
    @Mock
    private PersonalNotificationManager personalNotificationManager;
    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private AnalyticsContextProvider analyticsContextProvider;
    @Mock
    private AsyncExecutor asyncExecutor;
    @Mock
    private ChangeLogExtractor changeLogExtractor;

    @Mock
    private ApplicationUser applicationUser;
    @Mock
    private IssueEventBundle issueEventBundle;
    @Mock
    private DelegatingJiraIssueEvent jiraIssueEvent;
    @Mock
    private DelegatingJiraIssueEvent jiraIssueEvent2;
    @Mock
    private Issue issue;
    @Mock
    private NotificationInfo notificationInfo1;
    @Mock
    private NotificationInfo notificationInfo2;
    @Mock
    private NotificationInfo notificationInfo3;
    @Mock
    private SendNotificationTask sendNotificationTask;
    @Mock
    private SpanningOperation spanningOperation;

    @Captor
    private ArgumentCaptor<DefaultJiraIssueEvent> eventCaptor1;
    @Captor
    private ArgumentCaptor<DefaultJiraIssueEvent> eventCaptor2;
    @Captor
    private ArgumentCaptor<DefaultJiraIssueEvent> eventCaptor3;
    @Captor
    private ArgumentCaptor<List<NotificationInfo>> notInfoCaptor;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private DefaultJiraSlackEventListener target;

    @Test
    public void issueEvent_shouldTriggerNotificationSend() {
        IssueEvent issueEvent = new IssueEvent(issue, Collections.emptyMap(), applicationUser, 0L);
        when(applicationUser.getKey()).thenReturn("someUserKey");
        when(issueEventBundle.getEvents()).thenReturn(Collections.singletonList(jiraIssueEvent));
        when(jiraIssueEvent.asIssueEvent()).thenReturn(issueEvent);
        when(issueEventToEventMatcherTypeConverter.match(issueEvent))
                .thenReturn(Collections.singleton(EventMatcherType.ISSUE_UPDATED));
        when(dedicatedChannelManager.getNotificationsFor(eventCaptor1.capture()))
                .thenReturn(Optional.of(notificationInfo1));
        when(issueEventProcessorService.getNotificationsFor(eventCaptor2.capture()))
                .thenReturn(Arrays.asList(notificationInfo2, notificationInfo3));
        when(notificationInfo1.getChannelId()).thenReturn("C1");
        when(notificationInfo2.getChannelId()).thenReturn("C2");
        when(notificationInfo3.getChannelId()).thenReturn("C2");
        when(taskBuilder.newSendNotificationTask(eventCaptor3.capture(), notInfoCaptor.capture(), same(taskExecutorService)))
                .thenReturn(sendNotificationTask);
        CommonTestUtil.bypass(asyncExecutor);

        target.issueEvent(issueEventBundle);

        verify(sendNotificationTask).call();

        DefaultJiraIssueEvent defaultJiraIssueEvent = eventCaptor3.getValue();
        assertThat(defaultJiraIssueEvent.getEventMatcher(), sameInstance(EventMatcherType.ISSUE_UPDATED));
        assertThat(defaultJiraIssueEvent.getIssue(), sameInstance(issue));
        assertThat(defaultJiraIssueEvent.getEventAuthor().get(), sameInstance(applicationUser));

        assertThat(notInfoCaptor.getValue(), containsInAnyOrder(notificationInfo1, notificationInfo2));

        assertThat(eventCaptor1.getValue(), sameInstance(defaultJiraIssueEvent));
        assertThat(eventCaptor2.getValue(), sameInstance(defaultJiraIssueEvent));
    }

    @Test
    public void issueEvent_shouldSkipProcessingForBulkEditIfConfigured() {
        IssueEvent issueEvent = new IssueEvent(issue, applicationUser, null, null, null, Collections.emptyMap(), 0L,
                true, false, spanningOperation);
        String userKey = "someUserKey";
        when(applicationUser.getKey()).thenReturn(userKey);
        when(issueEventBundle.getEvents()).thenReturn(Collections.singletonList(jiraIssueEvent));
        when(jiraIssueEvent.asIssueEvent()).thenReturn(issueEvent);
        when(jiraSettingsService.areBulkNotificationsMutedForUser(argThat(arg -> arg.getStringValue().equals(userKey))))
            .thenReturn(true);

        target.issueEvent(issueEventBundle);

        verify(issueEventToEventMatcherTypeConverter, never()).match(any(IssueEvent.class));
        verify(taskBuilder, never()).newSendNotificationTask(any(), anyList(), any());
    }

    @Test
    public void issueEvent_shouldSkipProcessingDuplicateEvents() {
        long eventTypeId = EventType.ISSUE_ASSIGNED_ID;
        String userKey = "someUserKey";
        IssueEvent issueEvent = new IssueEvent(issue, Collections.emptyMap(), applicationUser, eventTypeId);
        IssueEvent issueEvent2 = new IssueEvent(issue, ImmutableMap.of(1, 1), applicationUser, eventTypeId);

        when(applicationUser.getKey()).thenReturn(userKey);
        when(issueEventBundle.getEvents()).thenReturn(Arrays.asList(jiraIssueEvent, jiraIssueEvent2));
        when(jiraIssueEvent.asIssueEvent()).thenReturn(issueEvent);
        when(jiraIssueEvent2.asIssueEvent()).thenReturn(issueEvent2);
        when(issueEventToEventMatcherTypeConverter.match(issueEvent))
                .thenReturn(Collections.singleton(EventMatcherType.ISSUE_ASSIGNMENT_CHANGED));
        when(issueEventToEventMatcherTypeConverter.match(issueEvent2))
                .thenReturn(Arrays.asList(EventMatcherType.ISSUE_UPDATED, EventMatcherType.ISSUE_ASSIGNMENT_CHANGED));
        when(issueEventProcessorService.getNotificationsFor(argThat(arg -> arg.getEventMatcher() == EventMatcherType.ISSUE_ASSIGNMENT_CHANGED)))
                .thenReturn(Collections.singleton(notificationInfo1));
        when(taskBuilder.newSendNotificationTask(any(PluginEvent.class), anyList(), eq(taskExecutorService)))
                .thenReturn(sendNotificationTask);
        CommonTestUtil.bypass(asyncExecutor);

        target.issueEvent(issueEventBundle);

        verify(sendNotificationTask).call();
        verify(taskBuilder, times(1)).newSendNotificationTask(any(PluginEvent.class), anyList(), eq(taskExecutorService));
    }

    @Test
    public void dedupNotificationsByChannel_shouldNotSkipPnNotifications() {
        List<NotificationInfo> notifications = Arrays.asList(
                new NotificationInfo(null, null, "user1", null, null, "", "", "", "", true, false, Verbosity.EXTENDED),
                new NotificationInfo(null, null, "user2", null, null, "", "", "", "", true, false, Verbosity.EXTENDED)
        );

        List<NotificationInfo> dedupped = target.dedupNotificationsByChannel(Optional.empty(), emptyList(), notifications);

        assertThat(new HashSet<>(dedupped), equalTo(new HashSet<>(notifications)));
    }
}
