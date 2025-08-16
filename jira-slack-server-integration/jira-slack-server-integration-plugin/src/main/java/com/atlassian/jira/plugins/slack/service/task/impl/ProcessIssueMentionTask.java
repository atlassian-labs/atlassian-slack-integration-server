package com.atlassian.jira.plugins.slack.service.task.impl;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugins.slack.model.SlackIncomingMessage;
import com.atlassian.jira.plugins.slack.service.mentions.IssueMentionService;
import jakarta.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Sends the notification to the specific channel.
 */
public class ProcessIssueMentionTask implements Runnable {
    private final IssueMentionService issueMentionService;
    private final Issue issue;
    private final SlackIncomingMessage message;

    ProcessIssueMentionTask(@Nonnull final IssueMentionService issueMentionService,
                            @Nonnull final Issue issue,
                            @Nonnull final SlackIncomingMessage message) {
        this.issueMentionService = checkNotNull(issueMentionService, "issueMentionService is null.");
        this.issue = checkNotNull(issue, "issue is null.");
        this.message = checkNotNull(message, "message is null.");
    }

    @Override
    public void run() {
        issueMentionService.issueMentioned(issue, message);
    }
}
