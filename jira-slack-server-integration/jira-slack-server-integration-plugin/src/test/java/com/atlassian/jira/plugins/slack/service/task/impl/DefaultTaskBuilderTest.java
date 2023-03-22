package com.atlassian.jira.plugins.slack.service.task.impl;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugins.slack.model.SlackDeletedMessage;
import com.atlassian.jira.plugins.slack.model.SlackIncomingMessage;
import com.atlassian.jira.plugins.slack.model.event.JiraCommandEvent;
import com.atlassian.jira.plugins.slack.model.event.PluginEvent;
import com.atlassian.jira.plugins.slack.service.mentions.IssueMentionService;
import com.atlassian.jira.plugins.slack.service.notification.EventRenderer;
import com.atlassian.jira.plugins.slack.service.notification.NotificationInfo;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.client.RetryLoaderHelper;
import com.atlassian.plugins.slack.api.client.SlackClient;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.atlassian.plugins.slack.user.SlackUserManager;
import com.atlassian.plugins.slack.util.AsyncExecutor;
import com.github.seratch.jslack.api.model.Conversation;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;

@PrepareForTest({DefaultTaskBuilder.class, SendNotificationTask.class})
@RunWith(PowerMockRunner.class)
public class DefaultTaskBuilderTest {
    @Mock
    private IssueMentionService issueMentionService;
    @Mock
    private EventRenderer eventRenderer;
    @Mock
    private SlackClientProvider slackClientProvider;
    @Mock
    private RetryLoaderHelper retryLoaderHelper;
    @Mock
    private SlackUserManager slackUserManager;

    @Mock
    private PluginEvent event;
    @Mock
    private NotificationInfo notificationInfo;
    @Mock
    private AsyncExecutor asyncExecutor;
    @Mock
    private Issue issue;
    @Mock
    private Conversation conversation;
    @Mock
    private SlackLink link;
    @Mock
    private SlackClient client;
    @Mock
    private SlackIncomingMessage slackMessage;
    @Mock
    private SlackDeletedMessage deletedMessage;
    @Mock
    private SendNotificationTask sendNotificationTask;
    @Mock
    private ProcessIssueMentionTask processIssueMentionTask;
    @Mock
    private ProcessMessageDeletedTask processMessageDeletedTask;
    @Mock
    private UnfurlIssueLinksTask unfurlIssueLinksTask;
    @Mock
    private DirectMessageTask directMessageTask;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private DefaultTaskBuilder target;

    @Test
    public void newSendNotificationTask_withList() throws Exception {
        PowerMockito.whenNew(SendNotificationTask.class).withArguments(eventRenderer, asyncExecutor, slackClientProvider,
                retryLoaderHelper, event, Collections.singletonList(notificationInfo)
        ).thenReturn(sendNotificationTask);

        SendNotificationTask result = target.newSendNotificationTask(event, Collections.singletonList(notificationInfo), asyncExecutor);

        assertThat(result, sameInstance(sendNotificationTask));
    }

    @Test
    public void newSendNotificationTask_singleItem() throws Exception {
        PowerMockito.whenNew(SendNotificationTask.class).withArguments(eventRenderer, asyncExecutor, slackClientProvider,
                retryLoaderHelper, event, Collections.singletonList(notificationInfo)
        ).thenReturn(sendNotificationTask);

        SendNotificationTask result = target.newSendNotificationTask(event, notificationInfo, asyncExecutor);

        assertThat(result, sameInstance(sendNotificationTask));
    }

    @Test
    public void newProcessIssueMentionTask() throws Exception {
        PowerMockito.whenNew(ProcessIssueMentionTask.class).withArguments(issueMentionService, issue, slackMessage)
                .thenReturn(processIssueMentionTask);

        ProcessIssueMentionTask result = target.newProcessIssueMentionTask(issue, slackMessage);

        assertThat(result, sameInstance(processIssueMentionTask));
    }

    @Test
    public void newProcessMessageDeletionTask() throws Exception {
        PowerMockito.whenNew(ProcessMessageDeletedTask.class).withArguments(issueMentionService, deletedMessage)
                .thenReturn(processMessageDeletedTask);

        ProcessMessageDeletedTask result = target.newProcessMessageDeletionTask(deletedMessage);

        assertThat(result, sameInstance(processMessageDeletedTask));
    }

    @Test
    public void newUnfurlIssueLinksTask() throws Exception {
        List<Pair<JiraCommandEvent, NotificationInfo>> notificationInfos = new ArrayList<>();
        PowerMockito.whenNew(UnfurlIssueLinksTask.class)
                .withArguments(eventRenderer, slackClientProvider, slackUserManager, notificationInfos)
                .thenReturn(unfurlIssueLinksTask);

        UnfurlIssueLinksTask result = target.newUnfurlIssueLinksTask(notificationInfos);

        assertThat(result, sameInstance(unfurlIssueLinksTask));
    }

    @Test
    public void newDirectMessageTask() throws Exception {
        PowerMockito.whenNew(DirectMessageTask.class).withArguments(eventRenderer, slackClientProvider, event, notificationInfo)
                .thenReturn(directMessageTask);

        DirectMessageTask result = target.newDirectMessageTask(event, notificationInfo);

        assertThat(result, sameInstance(directMessageTask));
    }
}
