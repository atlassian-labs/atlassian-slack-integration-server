package com.atlassian.jira.plugins.slack.manager.impl;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugins.slack.manager.IssueDetailsMessageManager;
import com.atlassian.jira.plugins.slack.model.DedicatedChannel;
import com.atlassian.jira.plugins.slack.model.analytics.JiraNotificationSentEvent;
import com.atlassian.jira.plugins.slack.model.analytics.JiraNotificationSentEvent.Type;
import com.atlassian.jira.plugins.slack.model.event.JiraCommandEvent;
import com.atlassian.jira.plugins.slack.model.event.ShowIssueEvent;
import com.atlassian.jira.plugins.slack.service.notification.NotificationInfo;
import com.atlassian.jira.plugins.slack.service.task.TaskBuilder;
import com.atlassian.plugins.slack.analytics.AnalyticsContextProvider;
import com.atlassian.plugins.slack.util.AsyncExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;

@Component
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class DefaultIssueDetailsMessageManager implements IssueDetailsMessageManager {
    private final TaskBuilder taskBuilder;
    private final AsyncExecutor asyncExecutor;
    private final EventPublisher eventPublisher;
    private final AnalyticsContextProvider analyticsContextProvider;

    /**
     * Send a message containing the details of the issue -- this is used when we autoconvert an issue key and when a
     * dedicated channel is first attached to an issue.
     */
    public void sendIssueDetailsMessageToChannel(final NotificationInfo notificationInfo,
                                                 final Issue issue,
                                                 @Nullable final DedicatedChannel dedicatedChannel) {
        eventPublisher.publish(new JiraNotificationSentEvent(analyticsContextProvider.byTeamIdAndSlackUserId(
                notificationInfo.getLink().getTeamId(), notificationInfo.getMessageAuthorId()), null, Type.UNFURLING));

        final JiraCommandEvent event = new ShowIssueEvent(issue, dedicatedChannel);
        asyncExecutor.run(taskBuilder.newSendNotificationTask(event, notificationInfo, asyncExecutor));
    }
}
