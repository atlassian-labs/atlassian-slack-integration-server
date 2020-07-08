package com.atlassian.jira.plugins.slack.service.mentions;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugins.slack.model.SlackIncomingMessage;
import com.atlassian.jira.plugins.slack.model.mentions.IssueMention;
import io.atlassian.fugue.Either;

import javax.annotation.Nonnull;
import java.util.List;

public interface IssueMentionService {
    void issueMentioned(@Nonnull Issue issue, @Nonnull SlackIncomingMessage message);

    void deleteMessageMention(String channelId, String messageTimestamp);

    @Nonnull
    Either<Throwable, List<IssueMention>> getIssueMentions(long issueId);
}
