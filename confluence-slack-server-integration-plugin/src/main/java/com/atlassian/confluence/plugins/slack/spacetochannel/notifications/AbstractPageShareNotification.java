package com.atlassian.confluence.plugins.slack.spacetochannel.notifications;

import com.atlassian.confluence.plugins.slack.spacetochannel.api.notifications.AttachmentBuilder;
import com.atlassian.confluence.plugins.slack.spacetochannel.model.ContentSharedEvent;
import com.atlassian.plugins.slack.api.events.NotificationBlockedEvent;
import com.atlassian.plugins.slack.api.notification.SlackUserActionNotification;
import com.atlassian.sal.api.message.I18nResolver;
import com.github.seratch.jslack.api.model.Attachment;

import java.util.Optional;

public class AbstractPageShareNotification extends BaseAbstractPageNotification<ContentSharedEvent> implements SlackUserActionNotification<ContentSharedEvent> {
    public AbstractPageShareNotification(final AttachmentBuilder contentCardBuilder,
                                         final I18nResolver i18nResolver) {
        super(contentCardBuilder, i18nResolver);
    }

    @Override
    public boolean supports(final Object event) {
        return event instanceof ContentSharedEvent;
    }

    @Override
    protected Optional<Attachment> getMessageAttachment(final ContentSharedEvent event) {
        return Optional.ofNullable(event.getAttachment());
    }

    @Override
    public Optional<NotificationBlockedEvent<ContentSharedEvent>> buildNotificationBlockedEvent(final ContentSharedEvent event) {
        NotificationBlockedEvent<ContentSharedEvent> notificationBlockedEvent = new NotificationBlockedEvent<>(event);
        return Optional.of(notificationBlockedEvent);
    }
}
