package com.atlassian.jira.plugins.slack.service.listener;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.plugins.slack.model.SlackDeletedMessage;
import com.atlassian.jira.plugins.slack.model.SlackIncomingMessage;
import com.atlassian.jira.plugins.slack.model.event.PluginEvent;
import com.atlassian.jira.plugins.slack.model.event.ShowAccountInfoEvent;
import com.atlassian.jira.plugins.slack.model.event.ShowHelpEvent;
import com.atlassian.jira.plugins.slack.model.event.ShowIssueNotFoundEvent;
import com.atlassian.jira.plugins.slack.service.notification.NotificationInfo;
import com.atlassian.jira.plugins.slack.service.task.TaskBuilder;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.notification.Verbosity;
import com.atlassian.plugins.slack.api.webhooks.GenericMessageSlackEvent;
import com.atlassian.plugins.slack.api.webhooks.LinkSharedSlackEvent;
import com.atlassian.plugins.slack.api.webhooks.SlackSlashCommand;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.plugins.slack.util.AsyncExecutor;
import com.atlassian.plugins.slack.util.AutoSubscribingEventListener;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.stream.Collectors;

import static com.atlassian.plugins.slack.util.SlackHelper.removeSlackLinks;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

/**
 * Event listener for Slack Jira issue mention events
 */
@Service
public class SlackEventListener extends AutoSubscribingEventListener {
    private static final Logger logger = LoggerFactory.getLogger(SlackEventListener.class);

    private final AsyncExecutor asyncExecutor;
    private final TaskBuilder taskBuilder;
    private final SlackLinkManager slackLinkManager;
    private final SlackEventHandlerService slackEventHandlerService;

    @Autowired
    SlackEventListener(final EventPublisher eventPublisher,
                       final AsyncExecutor asyncExecutor,
                       final TaskBuilder taskBuilder,
                       final SlackLinkManager slackLinkManager,
                       final SlackEventHandlerService slackEventHandlerService) {
        super(eventPublisher);
        this.asyncExecutor = asyncExecutor;
        this.taskBuilder = taskBuilder;
        this.slackLinkManager = slackLinkManager;
        this.slackEventHandlerService = slackEventHandlerService;
    }

    private PluginEvent handleCommand(final String commandText,
                                      final String slackUserId,
                                      final String botUserId,
                                      final String commandName) {
        if (commandText.isEmpty() || ShowHelpEvent.COMMAND.equalsIgnoreCase(commandText)) {
            return new ShowHelpEvent(botUserId, commandName);
        } else if (ShowAccountInfoEvent.COMMAND.equalsIgnoreCase(commandText)) {
            return new ShowAccountInfoEvent(slackUserId);
        }
        return new ShowIssueNotFoundEvent();
    }

    @EventListener
    public void slashCommand(@Nonnull final SlackSlashCommand command) {
        logger.debug("Got slash command {}", command.getCommandName());

        String commandText = StringUtils.trimToEmpty(command.getText());

        //try unfurl
        final boolean hasFoundAnyIssue = slackEventHandlerService.handleMessage(new SlackIncomingMessage(
                command.getTeamId(),
                command.getSlackLink(),
                command.getChannelId(),
                commandText,
                "",
                "",
                null,
                command.getUserId(),
                command.getResponseUrl(),
                false,
                false,
                true,
                Collections.emptyList()));

        if (!hasFoundAnyIssue) {
            final NotificationInfo notificationInfo = new NotificationInfo(
                    command.getSlackLink(),
                    command.getChannelId(),
                    command.getResponseUrl(),
                    null,
                    null,
                    Verbosity.EXTENDED);
            PluginEvent pluginEvent = handleCommand(commandText, command.getUserId(), command.getSlackLink().getBotUserId(),
                    command.getCommandName());
            asyncExecutor.run(taskBuilder.newSendNotificationTask(pluginEvent, notificationInfo, asyncExecutor));
        }
    }

    private boolean isSupportedSubtype(@Nullable final String subtype) {
        if(subtype == null) {
            return false;
        }
        return isBlank(subtype)
                || subtype.startsWith("file_comment")
                || subtype.startsWith("file_share")
                || subtype.equals("me_message")
                || subtype.equals("message_replied")
                || subtype.startsWith("thread_broadcast");
    }

    @EventListener
    public void messageReceived(@Nonnull final GenericMessageSlackEvent slackEvent) {
        logger.debug("Got message from Slack");

        final SlackLink slackLink = slackEvent.getSlackEvent().getSlackLink();

        if (slackEvent.isDeletedEvent()) {
            asyncExecutor.run(taskBuilder.newProcessMessageDeletionTask(new SlackDeletedMessage(
                    slackEvent.getSlackEvent().getTeamId(),
                    slackLink,
                    slackEvent.getChannel(),
                    slackEvent.getPreviousMessage().getTs()
            )));
        } else if (slackEvent.isChangedEvent()) {
            slackEventHandlerService.handleMessage(new SlackIncomingMessage(
                    slackEvent.getSlackEvent().getTeamId(),
                    slackLink,
                    slackEvent.getChannel(),
                    slackEvent.getMessage().getText(),
                    slackEvent.getPreviousMessage().getText(),
                    slackEvent.getMessage().getTs(),
                    slackEvent.getThreadTimestamp(),
                    slackEvent.getMessage().getUser(),
                    null,
                    true,
                    false,
                    false,
                    Collections.emptyList()));
        } else if (isSupportedSubtype(slackEvent.getSubtype()) && !slackEvent.isHidden()) {
            final String messageText = trimToEmpty(slackEvent.getText());
            final boolean hasFoundAnyIssue = slackEventHandlerService.handleMessage(new SlackIncomingMessage(
                    slackEvent.getSlackEvent().getTeamId(),
                    slackLink,
                    slackEvent.getChannel(),
                    messageText,
                    "",
                    slackEvent.getTs(),
                    slackEvent.getThreadTimestamp(),
                    slackEvent.getUser(),
                    null,
                    false,
                    false,
                    false,
                    Collections.emptyList()));

            // handle commands
            final boolean isDirectMessage = "im".equals(slackEvent.getChannelType());
            final boolean isMentioningBot = messageText.contains("@" + slackLink.getBotUserId());
            final boolean isThread = isNotBlank(slackEvent.getThreadTimestamp()); // ignore threaded direct message

            if (!hasFoundAnyIssue && !isThread && (isDirectMessage || isMentioningBot)) {
                final NotificationInfo notificationInfo = new NotificationInfo(
                        slackLink,
                        slackEvent.getChannel(),
                        slackEvent.getUser(),
                        !isDirectMessage);

                final PluginEvent commandEvent = handleCommand(removeSlackLinks(messageText), slackEvent.getUser(),
                        slackLink.getBotUserId(), null);

                asyncExecutor.run(taskBuilder.newSendNotificationTask(commandEvent, notificationInfo, asyncExecutor));
            }
        }
    }

    @EventListener
    public void linkShared(@Nonnull final LinkSharedSlackEvent slackEvent) {
        logger.debug("Got link shared");
        if (slackLinkManager.shouldUseLinkUnfurl(slackEvent.getSlackEvent().getTeamId())) {
            slackEventHandlerService.handleMessage(new SlackIncomingMessage(
                    slackEvent.getSlackEvent().getTeamId(),
                    slackEvent.getSlackEvent().getSlackLink(),
                    slackEvent.getChannel(),
                    "",
                    "",
                    slackEvent.getMessageTimestamp(),
                    slackEvent.getThreadTimestamp(),
                    slackEvent.getUser(),
                    null,
                    false,
                    true,
                    false,
                    slackEvent.getLinks().stream()
                            .map(LinkSharedSlackEvent.Link::getUrl)
                            .collect(Collectors.toList())));
        } else {
            logger.debug("Link unfurling disabled for team {}", slackEvent.getSlackEvent().getEventId());
        }
    }
}
