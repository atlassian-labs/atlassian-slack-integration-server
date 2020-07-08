package com.atlassian.bitbucket.plugins.slack.listener;

import com.atlassian.bitbucket.plugins.slack.event.RepositoryLinkedEvent;
import com.atlassian.bitbucket.plugins.slack.notification.renderer.SlackNotificationRenderer;
import com.atlassian.event.api.EventListener;
import com.atlassian.plugins.slack.api.client.SlackClient;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.atlassian.plugins.slack.util.AsyncExecutor;
import com.atlassian.plugins.slack.util.ErrorResponse;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest.ChatPostMessageRequestBuilder;
import io.atlassian.fugue.Either;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This class listens for {@link RepositoryLinkedEvent} and in response to these events it dispatches messages
 * to the Slack channel associated with the repository.
 */
@Component
public class RepositoryLinkedEventListener {
    private final SlackClientProvider slackClientProvider;
    private final SlackNotificationRenderer renderer;
    private final AsyncExecutor asyncExecutor;

    @Autowired
    public RepositoryLinkedEventListener(final SlackClientProvider slackClientProvider,
                                         final SlackNotificationRenderer renderer,
                                         final AsyncExecutor asyncExecutor) {
        this.slackClientProvider = slackClientProvider;
        this.renderer = renderer;
        this.asyncExecutor = asyncExecutor;
    }

    @EventListener
    public void onRepositoryLinked(final RepositoryLinkedEvent event) {
        asyncExecutor.run(() -> {
            final Either<Throwable, SlackClient> slackClient = slackClientProvider.withTeamId(event.getTeamId());

            slackClient
                    .flatMap(SlackClient::withRemoteUser)
                    .leftMap(ErrorResponse::new)
                    .forEach(client -> client.selfInviteToConversation(event.getChannelId()));

            final ChatPostMessageRequestBuilder message = renderer.getRepositoryLinkedMessage(event);
            slackClient.forEach(client -> client.postMessage(message.build()));
        });
    }
}
