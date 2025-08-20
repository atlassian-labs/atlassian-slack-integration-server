package com.atlassian.jira.plugins.slack.service.task.impl;

import com.atlassian.jira.plugins.slack.model.SlackDeletedMessage;
import com.atlassian.jira.plugins.slack.service.mentions.IssueMentionService;
import com.atlassian.plugins.slack.api.ConversationKey;
import jakarta.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Sends the notification to the specific channel.
 */
public class ProcessMessageDeletedTask implements Runnable {
    private final IssueMentionService issueMentionService;
    private final SlackDeletedMessage message;

    ProcessMessageDeletedTask(@Nonnull final IssueMentionService issueMentionService,
                              @Nonnull final SlackDeletedMessage message) {
        this.issueMentionService = checkNotNull(issueMentionService, "issueMentionService is null.");
        this.message = checkNotNull(message, "message is null.");
    }

    @Override
    public void run() {
        issueMentionService.deleteMessageMention(new ConversationKey(message.getTeamId(), message.getChannelId()), message.getTs());
    }
}
