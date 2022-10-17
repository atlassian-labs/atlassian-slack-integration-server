package com.atlassian.plugins.slack.api.client;

import com.atlassian.plugins.slack.util.ErrorResponse;
import com.google.gson.JsonParseException;
import io.atlassian.fugue.Either;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.function.Function;

@Service
public class RetryLoaderHelper {
    /**
     * Helps with retrying requests with various types of user tokens.
     *
     * @param baseSlackClient Slack client without any user token (i.e. bot token/default).
     * @param loader          function that loads some data using the client.
     * @param retryUsers      List of users to try.
     * @param <T>             Type to be loaded
     * @return Item loaded, if any
     */
    public <T> Optional<T> retryWithUserTokens(final SlackClient baseSlackClient,
                                               final Function<SlackClient, Either<ErrorResponse, T>> loader,
                                               final RetryUser... retryUsers) {
        for (RetryUser retryUser : retryUsers) {
            Optional<SlackClient> authenticatedClient = retryUser.withClient(baseSlackClient);
            Optional<Either<ErrorResponse, T>> response = authenticatedClient.map(loader);
            if (response.isPresent()) {
                Either<ErrorResponse, T> either = response.get();

                // successful request; return result, skip other retry alternatives
                if (either.isRight()) {
                    return either.toOptional();
                }

                // an error happened
                ErrorResponse errorResponse = either.left().get();
                Throwable exception = errorResponse.getException();

                // error parsing unsupported block ('rich_text' or other); ignore it and stop retrying
                // TODO: remove this workaround when REST client is upgraded to stop failing on unknown blocks
                if (exception instanceof JsonParseException &&
                        exception.getMessage().contains("Unsupported layout block type")) {
                    return Optional.empty();
                }
                // else some other error happened; keep retrying
            }
        }

        return Optional.empty();
    }
}
