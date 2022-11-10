package com.atlassian.confluence.plugins.slack.spacetochannel.notifications;

import com.atlassian.confluence.plugins.slack.spacetochannel.api.notifications.AttachmentBuilder;
import com.atlassian.confluence.plugins.slack.spacetochannel.api.notifications.ConfluenceSlackEvent;
import com.atlassian.confluence.plugins.slack.spacetochannel.api.notifications.SpaceToChannelNotification;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.sal.api.message.I18nResolver;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest.ChatPostMessageRequestBuilder;
import com.github.seratch.jslack.api.model.Attachment;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Represents a {@link com.atlassian.plugins.slack.api.notification.SlackNotification} that processes events that
 * are related to Confluence content.
 *
 * @param <T> the type of event
 */
public abstract class BaseAbstractPageNotification<T extends ConfluenceSlackEvent> implements SpaceToChannelNotification<T> {
    protected final AttachmentBuilder attachmentBuilder;
    private final I18nResolver i18nResolver;

    BaseAbstractPageNotification(final AttachmentBuilder attachmentBuilder,
                                 final I18nResolver i18nResolver) {
        this.attachmentBuilder = attachmentBuilder;
        this.i18nResolver = i18nResolver;
    }

    /**
     * Returns the i18n key of the message relating to the given event.
     */
    protected Optional<String> getActivityKey(T event) {
        return Optional.empty();
    }

    /**
     * Returns the i18n key of the message relating to the given event.
     */
    protected Optional<Attachment> getMessageAttachment(T event) {
        return Optional.empty();
    }

    @Override
    public boolean shouldDisplayInConfiguration() {
        return true;
    }

    @Override
    public boolean shouldSend(T event) {
        return true;
    }

    @Override
    public Optional<ChatPostMessageRequestBuilder> getSlackMessage(final T event) {
        final Optional<Attachment> attachment = getMessageAttachment(event);
        final Optional<String> activityKey = getActivityKey(event);
        if (!activityKey.isPresent() && !attachment.isPresent()) {
            return Optional.empty();
        }

        final String content = activityKey
                .map(key -> i18nResolver.getText(
                        activityKey.get(),
                        attachmentBuilder.userLink(event.getUser()),
                        event.getLink(),
                        attachmentBuilder.spaceLink(event.getSpace())))
                .orElse(null);

        final List<Attachment> attachments = attachment
                .map(Collections::singletonList)
                .orElseGet(Collections::emptyList);

        return Optional.of(ChatPostMessageRequest.builder()
                .mrkdwn(true)
                .attachments(attachments)
                .text(content));
    }

    @Override
    public Optional<Space> getSpace(final T event) {
        return Optional.ofNullable(event.getSpace());
    }
}
