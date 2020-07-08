package com.atlassian.plugins.slack.api.client;

import com.atlassian.plugins.slack.util.ErrorResponse;
import io.atlassian.fugue.Either;

public interface SlackLimitedClient {
    Either<ErrorResponse, Boolean> testApi();
}
