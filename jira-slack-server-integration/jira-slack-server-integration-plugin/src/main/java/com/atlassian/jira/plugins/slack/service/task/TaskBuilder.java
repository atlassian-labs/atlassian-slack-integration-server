package com.atlassian.jira.plugins.slack.service.task;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugins.slack.model.SlackDeletedMessage;
import com.atlassian.jira.plugins.slack.model.SlackIncomingMessage;
import com.atlassian.jira.plugins.slack.model.event.JiraCommandEvent;
import com.atlassian.jira.plugins.slack.model.event.PluginEvent;
import com.atlassian.jira.plugins.slack.service.notification.NotificationInfo;
import com.atlassian.jira.plugins.slack.service.task.impl.DirectMessageTask;
import com.atlassian.jira.plugins.slack.service.task.impl.ProcessIssueMentionTask;
import com.atlassian.jira.plugins.slack.service.task.impl.ProcessMessageDeletedTask;
import com.atlassian.jira.plugins.slack.service.task.impl.SendNotificationTask;
import com.atlassian.jira.plugins.slack.service.task.impl.UnfurlIssueLinksTask;
import com.atlassian.plugins.slack.util.AsyncExecutor;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

/**
 * Builder that will construct each task in our system.
 * <p>
 * This way we avoid passing all the dependencies inside all the classes
 */
public interface TaskBuilder {
    SendNotificationTask newSendNotificationTask(PluginEvent event,
                                                 List<NotificationInfo> notificationsInfo,
                                                 AsyncExecutor asyncExecutor);

    SendNotificationTask newSendNotificationTask(PluginEvent event,
                                                 NotificationInfo notificationInfo,
                                                 AsyncExecutor asyncExecutor);

    ProcessIssueMentionTask newProcessIssueMentionTask(Issue issue, SlackIncomingMessage incomingMessage);

    ProcessMessageDeletedTask newProcessMessageDeletionTask(SlackDeletedMessage deletedMessage);

    UnfurlIssueLinksTask newUnfurlIssueLinksTask(List<Pair<JiraCommandEvent, NotificationInfo>> notificationInfos);

    DirectMessageTask newDirectMessageTask(final PluginEvent event, final NotificationInfo notification);
}
