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
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class UnfurlIssueLinksTask implements Callable<Void> {
    private final EventRenderer eventRenderer;
    private final SlackClientProvider slackClientProvider;
    private final SlackUserManager slackUserManager;
    private final List<Pair<JiraCommandEvent, NotificationInfo>> notificationInfos;

    UnfurlIssueLinksTask(final EventRenderer eventRenderer,
                         final SlackClientProvider slackClientProvider,
                         final SlackUserManager slackUserManager,
                         final List<Pair<JiraCommandEvent, NotificationInfo>> notificationInfos) {
        this.eventRenderer = eventRenderer;
        this.slackClientProvider = slackClientProvider;
        this.slackUserManager = slackUserManager;
        this.notificationInfos = notificationInfos;
    }

    @Override
    public Void call() {
        if (!notificationInfos.isEmpty()) {
            // render attachments for every issue link
            Map<String, Attachment> unfurledIssueAttachments = new HashMap<>();
            for (Pair<JiraCommandEvent, NotificationInfo> notificationPair : notificationInfos) {
                NotificationInfo notificationInfo = notificationPair.getRight();
                final List<SlackNotification> renderedNotifications = eventRenderer.render(notificationPair.getLeft(),
                        Collections.singletonList(notificationInfo));
                SlackNotification renderedNotification = Iterables.getOnlyElement(renderedNotifications);
                List<Attachment> attachments = renderedNotification.getMessageRequest().getAttachments();
                String issueUrl = notificationInfo.getIssueUrl();
                unfurledIssueAttachments.put(issueUrl, Iterables.getOnlyElement(attachments));
            }

            // send multiple attachments in one request
            NotificationInfo notificationInfo = notificationInfos.get(0).getRight();
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
