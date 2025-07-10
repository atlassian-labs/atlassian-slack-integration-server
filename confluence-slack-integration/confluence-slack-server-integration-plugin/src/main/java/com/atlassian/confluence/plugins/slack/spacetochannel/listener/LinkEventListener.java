package com.atlassian.confluence.plugins.slack.spacetochannel.listener;

import com.atlassian.confluence.plugins.slack.spacetochannel.api.notifications.AttachmentBuilder;
import com.atlassian.confluence.plugins.slack.spacetochannel.service.SlackSpaceToChannelService;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.atlassian.plugins.slack.event.SlackLinkedEvent;
import com.atlassian.plugins.slack.event.SlackTeamUnlinkedEvent;
import com.atlassian.plugins.slack.util.AutoSubscribingEventListener;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Event listener for Slack Jira issue mention events
 */
@Service
public class LinkEventListener extends AutoSubscribingEventListener {
    private static final Logger logger = LoggerFactory.getLogger(LinkEventListener.class);

    private final SlackSpaceToChannelService slackSpaceToChannelService;
    private final SlackClientProvider slackClientProvider;
    private final AttachmentBuilder attachmentBuilder;

    @Autowired
    public LinkEventListener(final EventPublisher eventPublisher,
                             final SlackSpaceToChannelService slackSpaceToChannelService,
                             final SlackClientProvider slackClientProvider,
                             final AttachmentBuilder attachmentBuilder) {
        super(eventPublisher);
        this.slackSpaceToChannelService = slackSpaceToChannelService;
        this.slackClientProvider = slackClientProvider;
        this.attachmentBuilder = attachmentBuilder;
    }

    @EventListener
    public void linkWasDeleted(@Nonnull final SlackTeamUnlinkedEvent event) {
        logger.debug("Got SlackTeamUnlinkedEvent event");
        slackSpaceToChannelService.removeNotificationsForTeam(event.getTeamId());
    }

    @EventListener
    public void linkWasCreated(@Nonnull final SlackLinkedEvent event) {
        logger.debug("Got SlackLinkedEvent event");

        slackClientProvider.withLink(event.getLink()).postDirectMessage(
                event.getLink().getUserId(),
                ChatPostMessageRequest.builder()
                        .text(attachmentBuilder.getWelcomeMessage(event.getLink().getTeamId()))
                        .mrkdwn(true)
                        .build());
    }
}
