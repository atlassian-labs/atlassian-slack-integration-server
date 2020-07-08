package com.atlassian.jira.plugins.slack.manager.impl;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.plugins.slack.model.event.DedicatedChannelLinkedEvent;
import com.atlassian.jira.plugins.slack.model.event.DedicatedChannelUnlinkedEvent;
import com.atlassian.jira.plugins.slack.model.event.PluginEvent;
import com.atlassian.jira.plugins.slack.service.notification.NotificationInfo;
import com.atlassian.jira.plugins.slack.service.task.TaskBuilder;
import com.atlassian.jira.plugins.slack.service.task.TaskExecutorService;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.notification.Verbosity;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.plugins.slack.util.AutoSubscribingEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This component is responsible for sending messages to Slack in response to changes in dedicated channel assignments to issues
 * It has been separated from the DedicatedChannelManager implementation to fix circular dependencies
 */
@Component
public class DedicatedChannelEventListener extends AutoSubscribingEventListener {
    private final TaskExecutorService taskExecutorService;
    private final TaskBuilder taskBuilder;
    private final SlackLinkManager slackLinkManager;

    @Autowired
    public DedicatedChannelEventListener(final EventPublisher eventPublisher,
                                         final TaskExecutorService taskExecutorService,
                                         final TaskBuilder taskBuilder,
                                         final SlackLinkManager slackLinkManager) {
        super(eventPublisher);
        this.taskExecutorService = taskExecutorService;
        this.taskBuilder = taskBuilder;
        this.slackLinkManager = slackLinkManager;
    }

    @EventListener
    public void dedicatedChannelLinkedEventListener(final DedicatedChannelLinkedEvent event) {
        slackLinkManager.getLinkByTeamId(event.getTeamId()).forEach(link ->
                postNotification(link, event.getChannelId(), event.getOwner(), event));
    }

    @EventListener
    public void dedicatedChannelUnlinkedEventListener(final DedicatedChannelUnlinkedEvent event) {
        slackLinkManager.getLinkByTeamId(event.getTeamId()).forEach(link ->
                postNotification(link, event.getChannelId(), event.getOwner(), event));
    }

    private void postNotification(final SlackLink link,
                                  final String channelId,
                                  final String owner,
                                  final PluginEvent pluginEvent) {
        final NotificationInfo notificationInfo = new NotificationInfo(
                link,
                channelId,
                null,
                null,
                owner,
                Verbosity.EXTENDED);
        taskExecutorService.submitTask(taskBuilder.newSendNotificationTask(
                pluginEvent,
                notificationInfo,
                taskExecutorService));
    }
}
