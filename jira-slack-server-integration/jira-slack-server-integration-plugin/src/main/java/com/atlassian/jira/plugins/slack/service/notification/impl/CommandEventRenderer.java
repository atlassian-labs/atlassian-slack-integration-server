package com.atlassian.jira.plugins.slack.service.notification.impl;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.plugins.slack.model.DedicatedChannel;
import com.atlassian.jira.plugins.slack.model.SlackIncomingMessage;
import com.atlassian.jira.plugins.slack.model.SlackNotification;
import com.atlassian.jira.plugins.slack.model.event.IssueMentionedEvent;
import com.atlassian.jira.plugins.slack.model.event.JiraCommandEvent;
import com.atlassian.jira.plugins.slack.model.event.PluginEvent;
import com.atlassian.jira.plugins.slack.model.event.ShowAccountInfoEvent;
import com.atlassian.jira.plugins.slack.model.event.ShowBotAddedHelpEvent;
import com.atlassian.jira.plugins.slack.model.event.ShowHelpEvent;
import com.atlassian.jira.plugins.slack.model.event.ShowIssueEvent;
import com.atlassian.jira.plugins.slack.model.event.ShowIssueNotFoundEvent;
import com.atlassian.jira.plugins.slack.model.event.ShowWelcomeEvent;
import com.atlassian.jira.plugins.slack.service.notification.AttachmentHelper;
import com.atlassian.jira.plugins.slack.service.notification.NotificationInfo;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.plugins.slack.user.SlackUserManager;
import com.atlassian.plugins.slack.util.LinkHelper;
import com.atlassian.sal.api.message.I18nResolver;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest.ChatPostMessageRequestBuilder;
import com.github.seratch.jslack.api.model.Attachment;
import com.github.seratch.jslack.api.model.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import jakarta.ws.rs.core.UriBuilder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.atlassian.plugins.slack.util.SlackHelper.escapeSignsForSlackLink;
import static org.apache.commons.lang3.StringUtils.defaultString;

@Component("commandEventRenderer")
public class CommandEventRenderer extends AbstractEventRenderer<JiraCommandEvent> {
    private static final Logger log = LoggerFactory.getLogger(CommandEventRenderer.class);

    private final AttachmentHelper attachmentHelper;
    private final IssueManager issueManager;
    private final I18nResolver i18nResolver;
    private final SlackUserManager slackUserManager;
    private final UserManager userManager;

    @Autowired
    public CommandEventRenderer(final AttachmentHelper attachmentHelper,
                                final IssueManager issueManager,
                                final I18nResolver i18nResolver,
                                final SlackUserManager slackUserManager,
                                @Qualifier("jiraUserManager") final UserManager userManager) {
        this.attachmentHelper = attachmentHelper;
        this.issueManager = issueManager;
        this.i18nResolver = i18nResolver;
        this.slackUserManager = slackUserManager;
        this.userManager = userManager;
    }

    @Override
    protected List<SlackNotification> doRender(final JiraCommandEvent commandEvent,
                                               final List<NotificationInfo> notificationInfos) {
        return notificationInfos.stream()
                .flatMap(notificationInfo -> createNotification(commandEvent, notificationInfo)
                        .map(message -> Stream.of(new SlackNotification(
                                notificationInfo,
                                message
                                        .channel(notificationInfo.getChannelId())
                                        .threadTs(notificationInfo.getThreadTimestamp())
                                        .build()))
                        )
                        .orElseGet(Stream::empty)
                )
                .collect(Collectors.toList());
    }

    private Optional<ChatPostMessageRequestBuilder> createNotification(final JiraCommandEvent commandEvent,
                                                                       final NotificationInfo notificationInfo) {
        return commandEvent.accept(new JiraCommandEvent.Visitor<Optional<ChatPostMessageRequestBuilder>>() {
            @Override
            public Optional<ChatPostMessageRequestBuilder> visitIssueMentionedEvent(final IssueMentionedEvent event) {
                Issue issue = issueManager.getIssueObject(event.getIssueId());
                if (issue != null) {
                    return Optional.of(ChatPostMessageRequest.builder()
                            .mrkdwn(true)
                            .text(getMentionedMessage(event.getMessage(), notificationInfo))
                            .attachments(Collections.singletonList(
                                    Attachment.builder()
                                            .mrkdwnIn(Arrays.asList("text", "pretext"))
                                            .text(event.getMessage().getText())
                                            .build()
                            )));
                } else {
                    log.error("Didn't find issue for id " + event.getIssueId() + " while processing " + event);
                    return Optional.empty();
                }
            }

            /**
             * user mentioning an issue may be in a different team
             */
            private String getMentionedMessage(final SlackIncomingMessage slackIncomingMessage,
                                               final NotificationInfo notificationInfo) {
                if (slackIncomingMessage.getTeamId().equals(notificationInfo.getLink().getTeamId())) {
                    return i18nResolver.getText(
                            "jira.plugins.slack.dedicatedchannel.mention.attachment",
                            "@" + slackIncomingMessage.getUser(),
                            "#" + slackIncomingMessage.getChannelId());
                } else {
                    return i18nResolver.getText(
                            "jira.plugins.slack.dedicatedchannel.mention.attachment.external",
                            "https://slack.com/app_redirect?team=" + slackIncomingMessage.getTeamId()
                                    + "&channel=" + slackIncomingMessage.getChannelId());
                }
            }

            @Override
            public Optional<ChatPostMessageRequestBuilder> visitShowIssueEvent(final ShowIssueEvent event) {
                return Optional.of(ChatPostMessageRequest.builder()
                        .mrkdwn(true)
                        .attachments(Collections.singletonList(attachmentHelper.buildIssueAttachment(
                                null,
                                event.getIssue(),
                                event.getDedicatedChannel()
                                        .map(dedicatedChannel -> Collections.singletonList(Field.builder()
                                                .title(i18nResolver.getText("jira.plugins.slack.viewissue.panel.dedicated.channel.label"))
                                                .value(getDedicatedChannelLink(dedicatedChannel, notificationInfo))
                                                .build()))
                                        .orElseGet(Collections::emptyList)
                        ))));
            }

            @Override
            public Optional<ChatPostMessageRequestBuilder> visitShowIssueNotFoundEvent(final ShowIssueNotFoundEvent event) {
                return Optional.of(ChatPostMessageRequest.builder()
                        .text(i18nResolver.getText("jira.slack.card.notification.no.issues.found"))
                        .mrkdwn(true));
            }

            @Override
            public Optional<ChatPostMessageRequestBuilder> visitShowHelpEvent(final ShowHelpEvent event) {
                final String botMention = "<@" + event.getBotUserId() + ">";
                final String handleText = defaultString(event.getCommandName(), botMention);
                return Optional.of(ChatPostMessageRequest.builder()
                        .text(i18nResolver.getText(
                                "jira.slack.card.notification.show.help",
                                botMention,
                                jiraLinkWithTitle(),
                                handleText))
                        .mrkdwn(true));
            }

            @Override
            public Optional<ChatPostMessageRequestBuilder> visitShowWelcomeEvent(final ShowWelcomeEvent event) {
                return Optional.of(ChatPostMessageRequest.builder()
                        .text(":tada: " + i18nResolver.getText(
                                "jira.slack.card.notification.workspace.connected.welcome",
                                jiraLinkWithTitle(),
                                UriBuilder.fromUri(attachmentHelper.jiraUrl())
                                        .path("/plugins/servlet/slack/configure")
                                        .queryParam("teamId", event.getTeamId())
                                        .replaceQueryParam(LinkHelper.ATL_LINK_ORIGIN, null)
                                        .build()
                                        .toString()
                        ))
                        .mrkdwn(true));
            }

            @Override
            public Optional<ChatPostMessageRequestBuilder> visitShowBotAddedHelpEvent(final ShowBotAddedHelpEvent event) {
                return Optional.of(ChatPostMessageRequest.builder()
                        .text(i18nResolver.getText(
                                "jira.slack.card.notification.bot.added.to.channel.help",
                                jiraLinkWithTitle()
                        ))
                        .mrkdwn(true));
            }

            @Override
            public Optional<ChatPostMessageRequestBuilder> visitShowAccountInfoEvent(final ShowAccountInfoEvent event) {
                Optional<ApplicationUser> user = findJiraUserBySlackUserId(event.getSlackUserId());
                String messageText = attachmentHelper.getAccountMessage(user.orElse(null));
                return Optional.of(ChatPostMessageRequest.builder()
                        .text(messageText)
                        .mrkdwn(true));
            }

            private String jiraLinkWithTitle() {
                return attachmentHelper.jiraUrl() + "|" + escapeSignsForSlackLink(attachmentHelper.jiraTitle());
            }

            private Optional<ApplicationUser> findJiraUserBySlackUserId(final String slackUserId) {
                return slackUserManager.getBySlackUserId(slackUserId)
                        .map(slackUser -> userManager.getUserByKey(slackUser.getUserKey()));
            }

            /**
             * Dedicated channel may be in a different team
             */
            private String getDedicatedChannelLink(final DedicatedChannel dedicatedChannel,
                                                   final NotificationInfo notificationInfo) {
                if (dedicatedChannel.isPrivateChannel()) {
                    return "<https://slack.com/app_redirect?team=" +
                            dedicatedChannel.getTeamId() + "&channel=" +
                            dedicatedChannel.getChannelId() + "|" +
                            i18nResolver.getText("jira.slack.card.notification.private.channel.name") + ">";
                } else if (dedicatedChannel.getTeamId().equals(notificationInfo.getLink().getTeamId())) {
                    return "<#" + dedicatedChannel.getChannelId() + ">";
                } else {
                    return "<https://slack.com/app_redirect?team=" +
                            dedicatedChannel.getTeamId() + "&channel=" +
                            dedicatedChannel.getChannelId() + "|" +
                            escapeSignsForSlackLink(dedicatedChannel.getName()) + ">";
                }
            }
        });
    }

    @Override
    public boolean canRender(final PluginEvent pluginEvent) {
        return (pluginEvent instanceof JiraCommandEvent);
    }
}
