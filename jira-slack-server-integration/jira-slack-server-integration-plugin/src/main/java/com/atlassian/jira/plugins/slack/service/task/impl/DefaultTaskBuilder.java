package com.atlassian.jira.plugins.slack.service.task.impl;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugins.slack.model.SlackDeletedMessage;
import com.atlassian.jira.plugins.slack.model.SlackIncomingMessage;
import com.atlassian.jira.plugins.slack.model.event.PluginEvent;
import com.atlassian.jira.plugins.slack.service.mentions.IssueMentionService;
import com.atlassian.jira.plugins.slack.service.notification.EventRenderer;
import com.atlassian.jira.plugins.slack.service.notification.NotificationInfo;
import com.atlassian.jira.plugins.slack.service.task.TaskBuilder;
import com.atlassian.jira.plugins.slack.service.task.TaskExecutorService;
import com.atlassian.plugins.slack.api.client.RetryLoaderHelper;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.atlassian.plugins.slack.user.SlackUserManager;
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
                                                        final TaskExecutorService taskExecutorService) {
        return new SendNotificationTask(
                eventRenderer,
                event,
                notificationInfos,
                taskExecutorService,
                slackClientProvider,
                retryLoaderHelper);
    }

    @Override
    public SendNotificationTask newSendNotificationTask(final PluginEvent event,
                                                        final NotificationInfo notificationInfo,
                                                        final TaskExecutorService taskExecutorService) {
        return newSendNotificationTask(event, singletonList(notificationInfo), taskExecutorService);
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
    public UnfurlIssueLinksTask newUnfurlIssueLinksTask() {
        return new UnfurlIssueLinksTask(eventRenderer, slackClientProvider, slackUserManager);
    }

    @Override
    public DirectMessageTask newDirectMessageTask(final PluginEvent event, final NotificationInfo notification) {
        return new DirectMessageTask(eventRenderer, slackClientProvider, event, notification);
    }
}
