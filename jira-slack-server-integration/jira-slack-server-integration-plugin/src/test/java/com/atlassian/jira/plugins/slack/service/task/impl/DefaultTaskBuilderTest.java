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
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;

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
    private Conversation conversation;
    @Mock
    private SlackLink link;
    @Mock
    private SlackClient client;
    @Mock
    private SlackIncomingMessage slackMessage;
    @Mock
    private SlackDeletedMessage deletedMessage;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private DefaultTaskBuilder target;

    @Test
    public void newSendNotificationTask_withList() throws Exception {
        SendNotificationTask result = target.newSendNotificationTask(event, Collections.singletonList(notificationInfo), asyncExecutor);

        assertThat(result, org.hamcrest.Matchers.instanceOf(SendNotificationTask.class));
        assertThat(result, org.hamcrest.Matchers.notNullValue());
    }

    @Test
    public void newSendNotificationTask_singleItem() throws Exception {
        SendNotificationTask result = target.newSendNotificationTask(event, notificationInfo, asyncExecutor);

        assertThat(result, org.hamcrest.Matchers.instanceOf(SendNotificationTask.class));
        assertThat(result, org.hamcrest.Matchers.notNullValue());
    }

    @Test
    public void newProcessIssueMentionTask() throws Exception {
        ProcessIssueMentionTask result = target.newProcessIssueMentionTask(issue, slackMessage);

        assertThat(result, org.hamcrest.Matchers.instanceOf(ProcessIssueMentionTask.class));
        assertThat(result, org.hamcrest.Matchers.notNullValue());
    }

    @Test
    public void newProcessMessageDeletionTask() throws Exception {
        ProcessMessageDeletedTask result = target.newProcessMessageDeletionTask(deletedMessage);

        assertThat(result, org.hamcrest.Matchers.instanceOf(ProcessMessageDeletedTask.class));
        assertThat(result, org.hamcrest.Matchers.notNullValue());
    }

    @Test
    public void newUnfurlIssueLinksTask() throws Exception {
        List<Pair<JiraCommandEvent, NotificationInfo>> notificationInfos = new ArrayList<>();

        UnfurlIssueLinksTask result = target.newUnfurlIssueLinksTask(notificationInfos);

        assertThat(result, org.hamcrest.Matchers.instanceOf(UnfurlIssueLinksTask.class));
        assertThat(result, org.hamcrest.Matchers.notNullValue());
    }

    @Test
    public void newDirectMessageTask() throws Exception {
        DirectMessageTask result = target.newDirectMessageTask(event, notificationInfo);

        assertThat(result, org.hamcrest.Matchers.instanceOf(DirectMessageTask.class));
        assertThat(result, org.hamcrest.Matchers.notNullValue());
    }
}
