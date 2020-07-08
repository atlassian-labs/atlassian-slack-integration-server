package com.atlassian.confluence.plugins.slack.spacetochannel.notifications;

import com.atlassian.confluence.plugins.slack.spacetochannel.api.notifications.AttachmentBuilder;
import com.atlassian.confluence.plugins.slack.spacetochannel.model.AbstractPageEvent;
import com.atlassian.sal.api.message.I18nResolver;

import java.util.Optional;

public class BlogCreateContentNotification extends BaseAbstractPageNotification<AbstractPageEvent> {
    public BlogCreateContentNotification(final AttachmentBuilder contentCardBuilder,
                                         final I18nResolver i18nResolver) {
        super(contentCardBuilder, i18nResolver);
    }

    @Override
    public boolean supports(final Object event) {
        return event instanceof AbstractPageEvent && ((AbstractPageEvent) event).isBlogCreate();
    }

    @Override
    protected Optional<String> getActivityKey(final AbstractPageEvent event) {
        return Optional.of("slack.activity.blog-create");
    }
}
