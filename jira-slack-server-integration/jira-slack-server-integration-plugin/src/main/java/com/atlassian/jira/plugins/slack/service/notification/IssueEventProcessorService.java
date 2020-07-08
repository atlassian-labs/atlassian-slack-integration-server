package com.atlassian.jira.plugins.slack.service.notification;

import com.atlassian.jira.plugins.slack.model.event.JiraIssueEvent;

import java.util.Collection;

/**
 * In charge of processing issues and see if we have to send notifications or not
 */
public interface IssueEventProcessorService {
    /**
     * Returns the notifications for a given event
     *
     * @param event the event
     * @return a list of notifications
     */
    Collection<NotificationInfo> getNotificationsFor(JiraIssueEvent event);
}
