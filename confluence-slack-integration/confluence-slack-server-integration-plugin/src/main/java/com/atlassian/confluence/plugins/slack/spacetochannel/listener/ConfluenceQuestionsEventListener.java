package com.atlassian.confluence.plugins.slack.spacetochannel.listener;

import com.atlassian.confluence.content.CustomContentEntityObject;
import com.atlassian.confluence.content.event.PluginContentCreatedEvent;
import com.atlassian.confluence.plugins.slack.spacetochannel.api.notifications.AttachmentBuilder;
import com.atlassian.confluence.plugins.slack.spacetochannel.model.QuestionEvent;
import com.atlassian.confluence.plugins.slack.spacetochannel.model.QuestionType;
import com.atlassian.confluence.plugins.slack.spacetochannel.service.SlackContentPermissionChecker;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugins.slack.util.AutoSubscribingEventListener;
import com.github.seratch.jslack.api.model.Attachment;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class ConfluenceQuestionsEventListener extends AutoSubscribingEventListener {
    private final SlackContentPermissionChecker permissionChecker;
    private final AttachmentBuilder attachmentBuilder;

    @Autowired
    public ConfluenceQuestionsEventListener(final EventPublisher eventPublisher,
                                            final SlackContentPermissionChecker permissionChecker,
                                            final AttachmentBuilder attachmentBuilder) {
        super(eventPublisher);
        this.permissionChecker = permissionChecker;
        this.attachmentBuilder = attachmentBuilder;
    }

    @EventListener
    public void questionsEvent(final PluginContentCreatedEvent event) {
        if (QuestionType.isQuestionEvent(event.getContent())) {
            publishQuestionEvent(
                    event.getContent(),
                    QuestionType.QUESTION,
                    attachmentBuilder.pageLink(event.getContent()),
                    null);
        } else if (QuestionType.isAnswerEvent(event.getContent())) {
            publishQuestionEvent(
                    event.getContent(),
                    QuestionType.ANSWER,
                    attachmentBuilder.pageLink(event.getContent()),
                    Attachment.builder()
                            .text(StringUtils.abbreviate(event.getContent().getBodyAsStringWithoutMarkup(), 200))
                            .mrkdwnIn(Arrays.asList("text", "pretext"))
                            .build());
        }
    }

    private void publishQuestionEvent(final CustomContentEntityObject object,
                                      final QuestionType questionType,
                                      final String link,
                                      final Attachment attachment) {
        if (!permissionChecker.doesContentHaveViewRestrictions(object)) {
            eventPublisher.publish(new QuestionEvent(object.getSpace(), object.getCreator(), questionType, link, attachment));
        }
    }
}
