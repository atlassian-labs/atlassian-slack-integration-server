package com.atlassian.jira.plugins.slack.service.task;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugins.slack.model.SlackDeletedMessage;
import com.atlassian.jira.plugins.slack.model.SlackIncomingMessage;
import com.atlassian.jira.plugins.slack.model.event.JiraCommandEvent;
import com.atlassian.jira.plugins.slack.model.event.PluginEvent;
import com.atlassian.jira.plugins.slack.service.notification.NotificationInfo;
import com.atlassian.plugins.slack.util.AsyncExecutor;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

/**
 * Builder that will construct each task in our system.
 * <p>
 * This way we avoid passing all the dependencies inside all the classes
 */
public interface TaskBuilder {
    Runnable newSendNotificationTask(PluginEvent event, List<NotificationInfo> notificationsInfo, AsyncExecutor asyncExecutor);

    Runnable newSendNotificationTask(PluginEvent event, NotificationInfo notificationInfo, AsyncExecutor asyncExecutor);

    Runnable newProcessIssueMentionTask(Issue issue, SlackIncomingMessage incomingMessage);

    Runnable newProcessMessageDeletionTask(SlackDeletedMessage deletedMessage);

    Runnable newUnfurlIssueLinksTask(List<Pair<JiraCommandEvent, NotificationInfo>> notificationInfos);

    Runnable newDirectMessageTask(final PluginEvent event, final NotificationInfo notification);
}
