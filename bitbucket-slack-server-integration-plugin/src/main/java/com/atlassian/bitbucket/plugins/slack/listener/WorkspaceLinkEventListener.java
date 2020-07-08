package com.atlassian.bitbucket.plugins.slack.listener;

import com.atlassian.bitbucket.plugins.slack.notification.configuration.NotificationConfigurationService;
import com.atlassian.bitbucket.plugins.slack.notification.renderer.SlackNotificationRenderer;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.atlassian.plugins.slack.event.SlackLinkedEvent;
import com.atlassian.plugins.slack.event.SlackTeamUnlinkedEvent;
import com.atlassian.plugins.slack.util.AsyncExecutor;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;

/**
 * Event listener for Slack Jira issue mention events
 */
@Service
public class WorkspaceLinkEventListener {
    private static final Logger logger = LoggerFactory.getLogger(WorkspaceLinkEventListener.class);

    private final NotificationConfigurationService notificationConfigurationService;
    private final SlackClientProvider slackClientProvider;
    private final SlackNotificationRenderer slackNotificationRenderer;
    private final AsyncExecutor asyncExecutor;

    @Autowired
    public WorkspaceLinkEventListener(final EventPublisher eventPublisher,
                                      final NotificationConfigurationService notificationConfigurationService,
                                      final SlackClientProvider slackClientProvider,
                                      final SlackNotificationRenderer slackNotificationRenderer,
                                      final AsyncExecutor asyncExecutor) {
        this.notificationConfigurationService = notificationConfigurationService;
        this.slackClientProvider = slackClientProvider;
        this.slackNotificationRenderer = slackNotificationRenderer;
        this.asyncExecutor = asyncExecutor;
    }

    @EventListener
    public void linkWasDeleted(@Nonnull final SlackTeamUnlinkedEvent event) {
        logger.debug("Got SlackTeamUnlinkedEvent event");

        notificationConfigurationService.removeNotificationsForTeam(event.getTeamId());
    }

    @EventListener
    public void linkWasCreated(@Nonnull final SlackLinkedEvent event) {
        logger.debug("Got SlackLinkedEvent event");

        asyncExecutor.run(() -> slackClientProvider.withLink(event.getLink()).postDirectMessage(
                event.getLink().getUserId(),
                ChatPostMessageRequest.builder()
                        .text(slackNotificationRenderer.getWelcomeMessage(event.getLink().getTeamId()))
                        .mrkdwn(true)
                        .build()));
    }
}
