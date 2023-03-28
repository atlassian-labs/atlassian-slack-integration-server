package com.atlassian.jira.plugins.slack.service.task.impl;

import com.atlassian.jira.plugins.slack.model.SlackNotification;
import com.atlassian.jira.plugins.slack.model.event.PluginEvent;
import com.atlassian.jira.plugins.slack.service.notification.EventRenderer;
import com.atlassian.jira.plugins.slack.service.notification.NotificationInfo;
import com.atlassian.plugins.slack.api.client.SlackClient;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.google.common.collect.Iterables;

import java.util.Collections;
import java.util.List;

public class DirectMessageTask implements Runnable {
    private final EventRenderer eventRenderer;
    private final SlackClientProvider slackClientProvider;
    private final PluginEvent event;
    private final NotificationInfo notification;

    DirectMessageTask(final EventRenderer eventRenderer,
                      final SlackClientProvider slackClientProvider,
                      final PluginEvent event,
                      final NotificationInfo notification) {
        this.eventRenderer = eventRenderer;
        this.slackClientProvider = slackClientProvider;
        this.event = event;
        this.notification = notification;
    }

    @Override
    public void run() {
        final List<SlackNotification> messages = eventRenderer.render(event, Collections.singletonList(notification));
        SlackNotification message = Iterables.getOnlyElement(messages);
        final SlackClient client = slackClientProvider.withLink(message.getSlackLink());
        client.postDirectMessage(notification.getMessageAuthorId(), message.getMessageRequest());
    }
}
