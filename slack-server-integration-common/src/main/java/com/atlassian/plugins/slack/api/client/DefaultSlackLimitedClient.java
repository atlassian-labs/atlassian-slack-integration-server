package com.atlassian.plugins.slack.api.client;

import com.atlassian.plugins.slack.util.ErrorResponse;
import com.atlassian.plugins.slack.util.ResponseMapper;
import com.github.seratch.jslack.Slack;
import com.github.seratch.jslack.api.methods.request.api.ApiTestRequest;
import io.atlassian.fugue.Either;

public class DefaultSlackLimitedClient implements SlackLimitedClient {
    private final Slack slack;

    DefaultSlackLimitedClient(final Slack slack) {
        this.slack = slack;
    }

    @Override
    public Either<ErrorResponse, Boolean> testApi() {
        return ResponseMapper
                .toEither("api.test", () -> slack.methods().apiTest((ApiTestRequest.builder().build())))
                .map(v -> true);
    }
}
