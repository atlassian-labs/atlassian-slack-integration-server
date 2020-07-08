package com.atlassian.jira.plugins.slack.service.task;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugins.slack.model.SlackDeletedMessage;
import com.atlassian.jira.plugins.slack.model.SlackIncomingMessage;
import com.atlassian.jira.plugins.slack.model.event.PluginEvent;
import com.atlassian.jira.plugins.slack.service.notification.NotificationInfo;
import com.atlassian.jira.plugins.slack.service.task.impl.DirectMessageTask;
import com.atlassian.jira.plugins.slack.service.task.impl.ProcessIssueMentionTask;
import com.atlassian.jira.plugins.slack.service.task.impl.ProcessMessageDeletedTask;
import com.atlassian.jira.plugins.slack.service.task.impl.SendNotificationTask;
import com.atlassian.jira.plugins.slack.service.task.impl.UnfurlIssueLinksTask;

import java.util.List;

/**
 * Builder that will construct each task in our system.
 * <p>
 * This way we avoid passing all the dependencies inside all the classes
 */
public interface TaskBuilder {
    SendNotificationTask newSendNotificationTask(final PluginEvent event,
                                                 final List<NotificationInfo> notificationsInfo,
                                                 final TaskExecutorService taskExecutorService);

    SendNotificationTask newSendNotificationTask(final PluginEvent event,
                                                 final NotificationInfo notificationInfo,
                                                 final TaskExecutorService taskExecutorService);

    ProcessIssueMentionTask newProcessIssueMentionTask(Issue issue, SlackIncomingMessage incomingMessage);

    ProcessMessageDeletedTask newProcessMessageDeletionTask(SlackDeletedMessage deletedMessage);

    UnfurlIssueLinksTask newUnfurlIssueLinksTask();

    DirectMessageTask newDirectMessageTask(final PluginEvent event, final NotificationInfo notification);
}
