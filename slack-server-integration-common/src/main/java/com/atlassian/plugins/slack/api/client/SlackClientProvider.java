package com.atlassian.plugins.slack.api.client;

import com.atlassian.plugins.slack.api.SlackLink;
import io.atlassian.fugue.Either;

public interface SlackClientProvider {
    SlackClient withLink(SlackLink link);

    Either<Throwable, SlackClient> withTeamId(String teamId);

    SlackLimitedClient withoutCredentials();
}
