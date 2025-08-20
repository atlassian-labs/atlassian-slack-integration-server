package com.atlassian.jira.plugins.slack.service.notification.impl;

import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.plugins.slack.model.SlackNotification;
import com.atlassian.jira.plugins.slack.model.event.ConfigurationEvent;
import com.atlassian.jira.plugins.slack.model.event.DedicatedChannelLinkedEvent;
import com.atlassian.jira.plugins.slack.model.event.DedicatedChannelUnlinkedEvent;
import com.atlassian.jira.plugins.slack.model.event.PluginEvent;
import com.atlassian.jira.plugins.slack.model.event.ProjectMappingConfigurationEvent;
import com.atlassian.jira.plugins.slack.model.event.UnauthorizedUnfurlEvent;
import com.atlassian.jira.plugins.slack.service.notification.AttachmentHelper;
import com.atlassian.jira.plugins.slack.service.notification.NotificationInfo;
import com.atlassian.sal.api.message.I18nResolver;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest.ChatPostMessageRequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.ws.rs.core.UriBuilder;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.atlassian.plugins.slack.util.SlackHelper.escapeSignsForSlackLink;

@Component("configurationEventRenderer")
public class ConfigurationEventRenderer extends AbstractEventRenderer<ConfigurationEvent> {
    private static final Logger log = LoggerFactory.getLogger(ConfigurationEventRenderer.class);

    private final I18nResolver i18nResolver;
    private final AttachmentHelper attachmentHelper;
    private final IssueManager issueManager;

    @Autowired
    public ConfigurationEventRenderer(
            final I18nResolver i18nResolver,
            final AttachmentHelper attachmentHelper,
            final IssueManager issueManager) {
        this.i18nResolver = i18nResolver;
        this.attachmentHelper = attachmentHelper;
        this.issueManager = issueManager;
    }

    @Override
    protected List<SlackNotification> doRender(final ConfigurationEvent configurationEvent,
                                               final List<NotificationInfo> notificationInfos) {
        return notificationInfos.stream()
                .flatMap(notificationInfo -> renderMessage(configurationEvent)
                        .map(message -> Stream.of(new SlackNotification(
                                notificationInfo,
                                message
                                        .channel(notificationInfo.getChannelId())
                                        .threadTs(notificationInfo.getThreadTimestamp())
                                        .build())))
                        .orElseGet(Stream::empty)
                )
                .collect(Collectors.toList());
    }

    private Optional<ChatPostMessageRequestBuilder> renderMessage(final ConfigurationEvent configurationEvent) {
        return configurationEvent.accept(new ConfigurationEventCardCreator());
    }

    @Override
    public boolean canRender(final PluginEvent pluginEvent) {
        return (pluginEvent instanceof ConfigurationEvent);
    }

    private class ConfigurationEventCardCreator implements ConfigurationEvent.Visitor<Optional<ChatPostMessageRequestBuilder>> {
        @Override
        public Optional<ChatPostMessageRequestBuilder> visitDedicatedChannelLinkedEvent(final DedicatedChannelLinkedEvent event) {
            return Optional.of(ChatPostMessageRequest.builder()
                    .mrkdwn(true)
                    .attachments(Collections.singletonList(attachmentHelper.buildIssueAttachment(
                            i18nResolver.getText(
                                    "jira.plugins.slack.viewissue.panel.dedicated.channel.configured.notification.card.with.autoconvert",
                                    attachmentHelper.issueUrl(event.getIssueKey()) + "|" + event.getIssueKey()),
                            issueManager.getIssueByKeyIgnoreCase(event.getIssueKey()),
                            null
                    ))));
        }

        @Override
        public Optional<ChatPostMessageRequestBuilder> visitDedicatedChannelUnlinkedEvent(final DedicatedChannelUnlinkedEvent event) {
            return Optional.of(ChatPostMessageRequest.builder()
                    .mrkdwn(true)
                    .text(i18nResolver.getText(
                            "jira.plugins.slack.viewissue.panel.dedicated.channel.removed.notification.card",
                            attachmentHelper.issueUrl(event.getIssueKey()) + "|" + event.getIssueKey())));
        }

        @Override
        public Optional<ChatPostMessageRequestBuilder> visitProjectMappingConfigurationEvent(final ProjectMappingConfigurationEvent event) {
            final String projectUrl = attachmentHelper.projectUrl(event.getProjectKey());
            return getI18nKey(event, projectUrl, event.getProjectName()).flatMap(message -> Optional.of(ChatPostMessageRequest
                    .builder()
                    .mrkdwn(true)
                    .text(message)));
        }

        @Override
        public Optional<ChatPostMessageRequestBuilder> visitUnauthorizedUnfurlConfigurationEvent(UnauthorizedUnfurlEvent event) {
            String manageSlackConnectionsLink = UriBuilder.fromUri(attachmentHelper.jiraUrl())
                    .path("/plugins/servlet/slack/view-oauth-sessions")
                    .build()
                    .toString();
            String issueUrl = attachmentHelper.issueUrl(event.getIssueKey());
            String issueUrlFormatted = issueUrl + "|" + event.getIssueKey();
            String text = i18nResolver.getText("jira.slack.unauthenticated.unfurl.message",
                    issueUrlFormatted,
                    manageSlackConnectionsLink);
            return Optional.of(ChatPostMessageRequest.builder()
                    .mrkdwn(true)
                    .text(text));
        }

        private Optional<String> getI18nKey(final ProjectMappingConfigurationEvent event,
                                            final String projectUrl,
                                            final String projectName) {
            switch (event.getEventType()) {
                case CHANNEL_LINKED:
                    return Optional.of(i18nResolver.getText(
                            "jira.plugins.slack.channelmapping.add.notification.card",
                            attachmentHelper.userLink(event.getUser()),
                            projectUrl + "|" + escapeSignsForSlackLink(projectName)
                    ));
                case CHANNEL_UNLINKED:
                    return Optional.of(i18nResolver.getText(
                            "jira.plugins.slack.channelmapping.delete.notification.card",
                            projectUrl + "|" + escapeSignsForSlackLink(projectName)
                    ));
                default:
                    log.warn("Unexpected event type " + event.getEventType());
                    return Optional.empty();
            }
        }
    }
}
