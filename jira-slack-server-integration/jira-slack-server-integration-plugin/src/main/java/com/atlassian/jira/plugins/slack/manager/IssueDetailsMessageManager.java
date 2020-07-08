package com.atlassian.jira.plugins.slack.manager;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugins.slack.model.DedicatedChannel;
import com.atlassian.jira.plugins.slack.service.notification.NotificationInfo;

import javax.annotation.Nullable;

/**
 * Used to send plain issue details messages
 */
public interface IssueDetailsMessageManager {
    /**
     * Send a message containing the details of the issue.
     */
    void sendIssueDetailsMessageToChannel(NotificationInfo notificationInfo,
                                          Issue issue,
                                          @Nullable DedicatedChannel dedicatedChannel);
}
