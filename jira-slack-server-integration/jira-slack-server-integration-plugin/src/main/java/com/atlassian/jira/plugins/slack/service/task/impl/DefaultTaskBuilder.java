package com.atlassian.jira.plugins.slack.service.task.impl;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugins.slack.model.SlackDeletedMessage;
import com.atlassian.jira.plugins.slack.model.SlackIncomingMessage;
import com.atlassian.jira.plugins.slack.model.event.JiraCommandEvent;
import com.atlassian.jira.plugins.slack.model.event.PluginEvent;
import com.atlassian.jira.plugins.slack.service.mentions.IssueMentionService;
import com.atlassian.jira.plugins.slack.service.notification.EventRenderer;
import com.atlassian.jira.plugins.slack.service.notification.NotificationInfo;
import com.atlassian.jira.plugins.slack.service.task.TaskBuilder;
import com.atlassian.plugins.slack.api.client.RetryLoaderHelper;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.atlassian.plugins.slack.user.SlackUserManager;
import com.atlassian.plugins.slack.util.AsyncExecutor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.util.Collections.singletonList;

@Service
public class DefaultTaskBuilder implements TaskBuilder {
    private final IssueMentionService issueMentionService;
    private final EventRenderer eventRenderer;
    private final SlackClientProvider slackClientProvider;
    private final RetryLoaderHelper retryLoaderHelper;
    private final SlackUserManager slackUserManager;

    @Autowired
    public DefaultTaskBuilder(final IssueMentionService issueMentionService,
                              @Qualifier("eventRendererDispatcher") final EventRenderer eventRenderer,
                              final SlackClientProvider slackClientProvider,
                              final RetryLoaderHelper retryLoaderHelper,
                              final SlackUserManager slackUserManager) {
        this.issueMentionService = issueMentionService;
        this.eventRenderer = eventRenderer;
        this.slackClientProvider = slackClientProvider;
        this.retryLoaderHelper = retryLoaderHelper;
        this.slackUserManager = slackUserManager;
    }

    @Override
    public SendNotificationTask newSendNotificationTask(final PluginEvent event,
                                                        final List<NotificationInfo> notificationInfos,
                                                        final AsyncExecutor asyncExecutor) {
        return new SendNotificationTask(eventRenderer, asyncExecutor, slackClientProvider, retryLoaderHelper, event,
                notificationInfos);
    }

    @Override
    public SendNotificationTask newSendNotificationTask(final PluginEvent event,
                                                        final NotificationInfo notificationInfo,
                                                        final AsyncExecutor asyncExecutor) {
        return newSendNotificationTask(event, singletonList(notificationInfo), asyncExecutor);
    }

    @Override
    public ProcessIssueMentionTask newProcessIssueMentionTask(final Issue issue,
                                                              final SlackIncomingMessage slackMessage) {
        return new ProcessIssueMentionTask(issueMentionService, issue, slackMessage);
    }

    @Override
    public ProcessMessageDeletedTask newProcessMessageDeletionTask(final SlackDeletedMessage deletedMessage) {
        return new ProcessMessageDeletedTask(issueMentionService, deletedMessage);
    }

    @Override
    public UnfurlIssueLinksTask newUnfurlIssueLinksTask(final List<Pair<JiraCommandEvent, NotificationInfo>> notificationInfos) {
        return new UnfurlIssueLinksTask(eventRenderer, slackClientProvider, slackUserManager, notificationInfos);
    }

    @Override
    public DirectMessageTask newDirectMessageTask(final PluginEvent event, final NotificationInfo notification) {
        return new DirectMessageTask(eventRenderer, slackClientProvider, event, notification);
    }
}
