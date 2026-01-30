package com.atlassian.jira.plugins.slack.service.task.impl;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugins.slack.model.SlackDeletedMessage;
import com.atlassian.jira.plugins.slack.model.SlackIncomingMessage;
import com.atlassian.jira.plugins.slack.model.event.JiraCommandEvent;
import com.atlassian.jira.plugins.slack.model.event.PluginEvent;
import com.atlassian.jira.plugins.slack.service.mentions.IssueMentionService;
import com.atlassian.jira.plugins.slack.service.notification.EventRenderer;
import com.atlassian.jira.plugins.slack.service.notification.NotificationInfo;
import com.atlassian.plugins.slack.api.client.RetryLoaderHelper;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.atlassian.plugins.slack.user.SlackUserManager;
import com.atlassian.plugins.slack.util.AsyncExecutor;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
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
    private SlackIncomingMessage slackMessage;
    @Mock
    private SlackDeletedMessage deletedMessage;

    private DefaultTaskBuilder target;

    @Before
    public void setUp() {
        target = new DefaultTaskBuilder(issueMentionService, eventRenderer, slackClientProvider, retryLoaderHelper, slackUserManager);
    }

    @Test
    public void newSendNotificationTask_withList() {
        List<NotificationInfo> notificationInfos = List.of(notificationInfo);

        SendNotificationTask result = target.newSendNotificationTask(event, notificationInfos, asyncExecutor);

        SendNotificationTask expected = new SendNotificationTask(eventRenderer, asyncExecutor, slackClientProvider, retryLoaderHelper, event,
                notificationInfos);

        assertTrue(EqualsBuilder.reflectionEquals(result, expected));
    }

    @Test
    public void newSendNotificationTask_singleItem() {
        SendNotificationTask result = target.newSendNotificationTask(event, notificationInfo, asyncExecutor);

        SendNotificationTask expected = new SendNotificationTask(eventRenderer, asyncExecutor, slackClientProvider, retryLoaderHelper, event,
                List.of(notificationInfo));

        assertTrue(EqualsBuilder.reflectionEquals(result, expected));
    }

    @Test
    public void newProcessIssueMentionTask() {
        ProcessIssueMentionTask result = target.newProcessIssueMentionTask(issue, slackMessage);

        ProcessIssueMentionTask expected = new ProcessIssueMentionTask(issueMentionService, issue, slackMessage);

        assertTrue(EqualsBuilder.reflectionEquals(result, expected));
    }

    @Test
    public void newProcessMessageDeletionTask() {
        ProcessMessageDeletedTask result = target.newProcessMessageDeletionTask(deletedMessage);

        ProcessMessageDeletedTask expected = new ProcessMessageDeletedTask(issueMentionService, deletedMessage);

        assertTrue(EqualsBuilder.reflectionEquals(result, expected));
    }

    @Test
    public void newUnfurlIssueLinksTask() {
        List<Pair<JiraCommandEvent, NotificationInfo>> notificationInfos = new ArrayList<>();

        UnfurlIssueLinksTask result = target.newUnfurlIssueLinksTask(notificationInfos);

        UnfurlIssueLinksTask expected = new UnfurlIssueLinksTask(eventRenderer, slackClientProvider, slackUserManager, notificationInfos);

        assertTrue(EqualsBuilder.reflectionEquals(result, expected));
    }

    @Test
    public void newDirectMessageTask() {
        DirectMessageTask result = target.newDirectMessageTask(event, notificationInfo);

        DirectMessageTask expected = new DirectMessageTask(eventRenderer, slackClientProvider, event, notificationInfo);

        assertTrue(EqualsBuilder.reflectionEquals(result, expected));
    }
}
