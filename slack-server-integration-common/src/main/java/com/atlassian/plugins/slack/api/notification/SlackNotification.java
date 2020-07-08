package com.atlassian.plugins.slack.api.notification;

import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest.ChatPostMessageRequestBuilder;

import java.util.Optional;

/**
 * This represents the module that processes an event and
 * produces a message that is sent to a Slack channel
 *
 * @param <T> the event class that the notification processes.
 */
public interface SlackNotification<T> {
    boolean supports(Object event);

    boolean shouldSend(T event);

    boolean shouldDisplayInConfiguration();

    Optional<ChatPostMessageRequestBuilder> getSlackMessage(T event);
}
