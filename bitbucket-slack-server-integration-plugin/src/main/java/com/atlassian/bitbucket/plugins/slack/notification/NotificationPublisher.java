package com.atlassian.bitbucket.plugins.slack.notification;

import com.atlassian.bitbucket.plugins.slack.event.analytic.BitbucketNotificationSentEvent;
import com.atlassian.bitbucket.plugins.slack.event.analytic.BitbucketNotificationSentEvent.Type;
import com.atlassian.bitbucket.plugins.slack.model.ExtendedChannelToNotify;
import com.atlassian.bitbucket.plugins.slack.model.NotificationRenderingOptions;
import com.atlassian.bitbucket.plugins.slack.notification.configuration.NotificationConfigurationService;
import com.atlassian.bitbucket.plugins.slack.notification.configuration.NotificationSearchRequest;
import com.atlassian.bitbucket.plugins.slack.settings.BitbucketSlackSettingsService;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugins.slack.analytics.AnalyticsContext;
import com.atlassian.plugins.slack.analytics.AnalyticsContextProvider;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.atlassian.plugins.slack.api.notification.ChannelToNotify;
import com.atlassian.plugins.slack.api.notification.SlackNotification;
import com.atlassian.plugins.slack.api.notification.SlackNotificationContext;
import com.atlassian.plugins.slack.api.notification.Verbosity;
import com.atlassian.plugins.slack.util.AsyncExecutor;
import com.atlassian.plugins.slack.util.ErrorResponse;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest.ChatPostMessageRequestBuilder;
import io.atlassian.fugue.Functions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * This class listens to all the events and if there is a {@link SlackNotification} that can handle this event the
 * corresponding {@link SlackNotificationContext} is called to get the channel IDs and then the slack message generated
 * by {@link SlackNotification} is send to these channels.
 */
@Slf4j
@Component
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class NotificationPublisher {
    private final SlackClientProvider slackClientProvider;
    private final NotificationConfigurationService notificationConfigurationService;
    private final AsyncExecutor asyncExecutor;
    private final BitbucketSlackSettingsService bitbucketSlackSettingsService;
    private final EventPublisher eventPublisher;
    private final AnalyticsContextProvider analyticsContextProvider;

    public void findChannelsAndPublishNotificationsAsync(
            final Repository repository,
            final String notificationTypeKey,
            final Supplier<Set<ExtendedChannelToNotify>> personalNotificationsProvider,
            final Function<NotificationRenderingOptions, Optional<ChatPostMessageRequestBuilder>> messageProvider) {
        asyncExecutor.run(() -> {
            final Function<NotificationRenderingOptions, Optional<ChatPostMessageRequestBuilder>> cachedMessageProvider = Functions.weakMemoize(messageProvider);
            notify(getChannelsToNotify(repository, notificationTypeKey), repository, cachedMessageProvider);
            notify(personalNotificationsProvider.get(), repository, cachedMessageProvider);
        });
    }

    private void notify(final Collection<ExtendedChannelToNotify> channelsToNotify,
                        final Repository repository,
                        final Function<NotificationRenderingOptions, Optional<ChatPostMessageRequestBuilder>> messageProvider) {
        channelsToNotify.forEach(channel -> {
            ChannelToNotify channelToNotify = channel.getChannel();
            final Verbosity verbosity = bitbucketSlackSettingsService.getVerbosity(
                    repository.getId(), channelToNotify.getTeamId(), channelToNotify.getChannelId());
            messageProvider
                    .apply(new NotificationRenderingOptions(verbosity, channelToNotify.isPersonal(), channel.getApplicationUser()))
                    .ifPresent(message -> {
                        AnalyticsContext context = analyticsContextProvider.byTeamId(channelToNotify.getTeamId());
                        eventPublisher.publish(new BitbucketNotificationSentEvent(context, channel.getNotificationKey(),
                                channelToNotify.isPersonal() ? Type.PERSONAL : Type.REGULAR));
                        sendMessageAsync(channel.getChannel(), message);
                    });
        });
    }

    private Set<ExtendedChannelToNotify> getChannelsToNotify(final Repository repository,
                                                             final String notificationTypeKey) {
        final NotificationSearchRequest request = new NotificationSearchRequest.Builder()
                .repository(repository)
                .notificationType(notificationTypeKey)
                .build();
        Set<ChannelToNotify> channels = notificationConfigurationService.getChannelsToNotify(request);
        return channels.stream()
                .map(channel -> new ExtendedChannelToNotify(channel, notificationTypeKey, null))
                .collect(Collectors.toSet());
    }

    public void sendMessageAsync(final String teamId,
                                 final String channelId,
                                 final ChatPostMessageRequestBuilder message) {
        sendMessageAsync(new ChannelToNotify(teamId, channelId, null, false), message);
    }

    private void sendMessageAsync(final ChannelToNotify channelToNotify,
                                  final ChatPostMessageRequestBuilder message) {
        asyncExecutor.run(() -> slackClientProvider.withTeamId(channelToNotify.getTeamId())
                .leftMap(ErrorResponse::new)
                .flatMap(client -> {
                    if (channelToNotify.isPersonal()) {
                        return client.postDirectMessage(channelToNotify.getChannelId(), message.mrkdwn(true).build());
                    } else {
                        return client.postMessage(message.mrkdwn(true).channel(channelToNotify.getChannelId()).build());
                    }
                })
                .left()
                .forEach(e -> {
                    log.warn("Unable to send Slack Notification. Reason: {}", e.getMessage());
                    if (log.isDebugEnabled()) {
                        log.debug("Detailed exception: ", e.getException());
                    }
                }));
    }
}
