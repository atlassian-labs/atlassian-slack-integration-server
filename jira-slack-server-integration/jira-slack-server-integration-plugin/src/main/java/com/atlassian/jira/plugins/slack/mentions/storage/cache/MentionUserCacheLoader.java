package com.atlassian.jira.plugins.slack.mentions.storage.cache;

import com.atlassian.jira.plugins.slack.model.UserId;
import com.atlassian.jira.plugins.slack.model.mentions.MentionUser;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.atlassian.plugins.slack.util.ErrorResponse;
import com.github.seratch.jslack.api.model.User;
import jakarta.annotation.Nonnull;

import java.util.Optional;

public class MentionUserCacheLoader extends SlackApiCacheLoader<UserId, MentionUser, User> {
    MentionUserCacheLoader(@Nonnull final SlackClientProvider slack) {
        super(slack);
    }

    @Override
    protected MentionUser createCacheableEntity(final UserId key, final User load) {
        return new MentionUser(load);
    }

    @Override
    protected Optional<User> fetchCacheLoad(final UserId key) {
        return getSlackClientProvider()
                .withTeamId(key.getTeamId())
                .leftMap(ErrorResponse::new)
                .flatMap(client -> client.getUserInfo(key.getUserId()))
                .toOptional();
    }
}
