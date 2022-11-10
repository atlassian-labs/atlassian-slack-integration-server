package com.atlassian.confluence.plugins.slack.spacetochannel.notifications;

import com.atlassian.confluence.plugins.slack.spacetochannel.api.notifications.AttachmentBuilder;
import com.atlassian.confluence.plugins.slack.spacetochannel.model.QuestionEvent;
import com.atlassian.confluence.plugins.slack.spacetochannel.model.QuestionType;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.sal.api.message.I18nResolver;
import com.github.seratch.jslack.api.model.Attachment;

import java.util.Optional;

public class QuestionContentNotification extends BaseAbstractPageNotification<QuestionEvent> {
    private final PluginAccessor pluginAccessor;

    public QuestionContentNotification(final AttachmentBuilder contentCardBuilder,
                                       final I18nResolver i18nResolver,
                                       final PluginAccessor pluginAccessor) {
        super(contentCardBuilder, i18nResolver);
        this.pluginAccessor = pluginAccessor;
    }

    @Override
    public boolean supports(final Object event) {
        return event instanceof QuestionEvent && !((QuestionEvent) event).isAnswer();
    }

    @Override
    public boolean shouldDisplayInConfiguration() {
        try {
            return pluginAccessor.isPluginModuleEnabled(QuestionType.QUESTION.pluginModuleKey());
        } catch (Throwable e) {
            return false;
        }
    }

    @Override
    protected Optional<String> getActivityKey(final QuestionEvent event) {
        return Optional.of("slack.activity.question-asked");
    }

    @Override
    protected Optional<Attachment> getMessageAttachment(final QuestionEvent event) {
        return Optional.ofNullable(event.getAttachment());
    }
}
