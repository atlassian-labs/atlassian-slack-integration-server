package com.atlassian.plugins.slack.api.client;

import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

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
                                               final Function<SlackClient, Optional<T>> loader,
                                               final RetryUser... retryUsers) {
        return Stream.of(retryUsers)
                .map(retry -> retry.withClient(baseSlackClient))
                .flatMap(clientOptional -> clientOptional.flatMap(loader).map(Stream::of).orElseGet(Stream::empty))
                .findFirst();
    }
}
