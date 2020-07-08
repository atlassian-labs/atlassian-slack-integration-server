package com.atlassian.jira.plugins.slack.service.task.impl;

import com.atlassian.jira.plugins.slack.model.SlackNotification;
import com.atlassian.jira.plugins.slack.model.event.JiraCommandEvent;
import com.atlassian.jira.plugins.slack.service.notification.EventRenderer;
import com.atlassian.jira.plugins.slack.service.notification.NotificationInfo;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.atlassian.plugins.slack.user.SlackUserManager;
import com.github.seratch.jslack.api.model.Attachment;
import com.google.common.collect.Iterables;
import io.atlassian.fugue.Pair;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class UnfurlIssueLinksTask implements Callable<Void> {
    private final EventRenderer eventRenderer;
    private final SlackClientProvider slackClientProvider;
    private final SlackUserManager slackUserManager;

    @Getter
    private final List<Pair<JiraCommandEvent, NotificationInfo>> notifications = new ArrayList<>();

    UnfurlIssueLinksTask(final EventRenderer eventRenderer,
                         final SlackClientProvider slackClientProvider,
                         final SlackUserManager slackUserManager) {
        this.eventRenderer = eventRenderer;
        this.slackClientProvider = slackClientProvider;
        this.slackUserManager = slackUserManager;
    }

    public void addNotification(final JiraCommandEvent event, final NotificationInfo notificationInfo) {
        notifications.add(Pair.pair(event, notificationInfo));
    }

    @Override
    public Void call() {
        if (!notifications.isEmpty()) {
            // render attachments for every issue link
            Map<String, Attachment> unfurledIssueAttachments = new HashMap<>();
            for (Pair<JiraCommandEvent, NotificationInfo> notificationPair : notifications) {
                NotificationInfo notificationInfo = notificationPair.right();
                final List<SlackNotification> renderedNotifications = eventRenderer.render(notificationPair.left(),
                        Collections.singletonList(notificationInfo));
                SlackNotification renderedNotification = Iterables.getOnlyElement(renderedNotifications);
                List<Attachment> attachments = renderedNotification.getMessageRequest().getAttachments();
                String issueUrl = notificationInfo.getIssueUrl();
                unfurledIssueAttachments.put(issueUrl, Iterables.getOnlyElement(attachments));
            }

            // send multiple attachments in one request
            NotificationInfo notificationInfo = notifications.get(0).right();
            SlackLink link = notificationInfo.getLink();
            String channelId = notificationInfo.getChannelId();
            String messageTimestamp = notificationInfo.getMessageTimestamp();

            slackUserManager.getBySlackUserId(notificationInfo.getMessageAuthorId())
                    .flatMap(user -> slackClientProvider.withLink(link).withUserTokenIfAvailable(user))
                    .ifPresent(client -> client.unfurl(channelId, messageTimestamp, unfurledIssueAttachments));
        }

        return null;
    }
}
