package com.atlassian.confluence.plugins.slack.spacetochannel.listener;

import com.atlassian.confluence.content.ContentEntityExcerpter;
import com.atlassian.confluence.core.ContentEntityObject;
import com.atlassian.confluence.core.SpaceContentEntityObject;
import com.atlassian.confluence.event.events.content.ContentEvent;
import com.atlassian.confluence.event.events.content.blogpost.BlogPostCreateEvent;
import com.atlassian.confluence.event.events.content.blogpost.BlogPostUpdateEvent;
import com.atlassian.confluence.event.events.content.comment.CommentCreateEvent;
import com.atlassian.confluence.event.events.content.page.PageCreateEvent;
import com.atlassian.confluence.event.events.content.page.PageUpdateEvent;
import com.atlassian.confluence.pages.AbstractPage;
import com.atlassian.confluence.plugins.slack.spacetochannel.api.notifications.AttachmentBuilder;
import com.atlassian.confluence.plugins.slack.spacetochannel.model.AbstractPageEvent;
import com.atlassian.confluence.plugins.slack.spacetochannel.model.EventType;
import com.atlassian.confluence.plugins.slack.spacetochannel.model.PageType;
import com.atlassian.confluence.plugins.slack.spacetochannel.service.PersonalNotificationService;
import com.atlassian.confluence.plugins.slack.spacetochannel.service.SlackContentPermissionChecker;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugins.slack.util.AutoSubscribingEventListener;
import com.atlassian.sal.api.message.I18nResolver;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest.ChatPostMessageRequestBuilder;
import com.github.seratch.jslack.api.model.block.SectionBlock;
import com.github.seratch.jslack.api.model.block.composition.MarkdownTextObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static java.util.Collections.singletonList;

@Component
public class ConfluenceEventListener extends AutoSubscribingEventListener {
    private final SlackContentPermissionChecker permissionChecker;
    private final AttachmentBuilder attachmentBuilder;
    private final PersonalNotificationService personalNotificationService;
    private final I18nResolver i18nResolver;

    @Autowired
    public ConfluenceEventListener(final EventPublisher eventPublisher,
                                   final SlackContentPermissionChecker permissionChecker,
                                   final AttachmentBuilder attachmentBuilder,
                                   final PersonalNotificationService personalNotificationService,
                                   final I18nResolver i18nResolver) {
        super(eventPublisher);
        this.permissionChecker = permissionChecker;
        this.attachmentBuilder = attachmentBuilder;
        this.personalNotificationService = personalNotificationService;
        this.i18nResolver = i18nResolver;
    }

    @EventListener
    public void pageCreateEvent(final PageCreateEvent event) {
        if (event.isSuppressNotifications()) {
            return;
        }
        createAbstractPageEventIfNotRestricted(event.getPage(), EventType.CREATE, PageType.PAGE)
                .ifPresent(notificationEvent -> publishEvent(notificationEvent, event));
    }

    @EventListener
    public void pageUpdateEvent(final PageUpdateEvent event) {
        if (event.isSuppressNotifications()) {
            return;
        }
        createAbstractPageEventIfNotRestricted(event.getPage(), EventType.UPDATE, PageType.PAGE)
                .ifPresent(notificationEvent -> publishEvent(notificationEvent, event));
    }

    @EventListener
    public void blogPostCreateEvent(final BlogPostCreateEvent event) {
        if (event.isSuppressNotifications()) {
            return;
        }
        createAbstractPageEventIfNotRestricted(event.getBlogPost(), EventType.CREATE, PageType.BLOG)
                .ifPresent(notificationEvent -> publishEvent(notificationEvent, event));
    }

    @EventListener
    public void blogPostCreateEvent(final BlogPostUpdateEvent event) {
        if (event.isSuppressNotifications()) {
            return;
        }
        createAbstractPageEventIfNotRestricted(event.getBlogPost(), EventType.UPDATE, PageType.BLOG)
                .ifPresent(notificationEvent -> publishEvent(notificationEvent, event));
    }

    @EventListener
    public void commentCreated(final CommentCreateEvent event) {
        if (event.isSuppressNotifications()) {
            return;
        }
        final ContentEntityObject content = event.getComment().getContainer();
        if (content instanceof SpaceContentEntityObject spaceContent
                && !permissionChecker.doesContentHaveViewRestrictions(content)) {
            String commentText = new ContentEntityExcerpter().getBodyAsStringWithoutMarkup(event.getComment())
                    .orElse(null);
            personalNotificationService.notifyForComment(
                    event.getComment().getCreator(),
                    spaceContent,
                    () -> buildSimpleCommentNotification(
                            event.getComment().getCreator(),
                            commentText,
                            spaceContent,
                            spaceContent.getSpace()));
        }
    }

    private void publishEvent(final AbstractPageEvent notificationEvent, final ContentEvent event) {
        eventPublisher.publish(notificationEvent);
        personalNotificationService.notifyForContent(
                notificationEvent.getUser(),
                (AbstractPage) event.getContent(),
                () -> buildSimplePageNotification(notificationEvent));
    }

    private Optional<AbstractPageEvent> createAbstractPageEventIfNotRestricted(final AbstractPage object,
                                                                               final EventType eventType,
                                                                               final PageType pageType) {
        if (!permissionChecker.doesContentHaveViewRestrictions(object)) {
            return Optional.of(new AbstractPageEvent(object, eventType, pageType, attachmentBuilder.pageLink(object)));
        }
        return Optional.empty();
    }

    private ChatPostMessageRequestBuilder buildSimplePageNotification(final AbstractPageEvent event) {
        return ChatPostMessageRequest.builder()
                .mrkdwn(true)
                .text(i18nResolver.getText(
                        "slack.activity." + event.getPageType().name().toLowerCase() + "-" + event.getEventType().name().toLowerCase(),
                        attachmentBuilder.userLink(event.getUser()),
                        event.getLink(),
                        attachmentBuilder.spaceLink(event.getSpace())));
    }


    private ChatPostMessageRequestBuilder buildSimpleCommentNotification(final ConfluenceUser user,
                                                                         final String commentText,
                                                                         final SpaceContentEntityObject content,
                                                                         final Space space) {
        final String headLine = i18nResolver.getText(
                "slack.activity.comment-create",
                attachmentBuilder.userLink(user),
                attachmentBuilder.pageLink(content),
                attachmentBuilder.spaceLink(space));
        return ChatPostMessageRequest.builder()
                .mrkdwn(true)
                .blocks(singletonList(SectionBlock.builder()
                        .text(MarkdownTextObject.builder()
                                .text(StringUtils.abbreviate(headLine + "\n>>>" + commentText, 1000))
                                .build())
                        .build()))
                .text(headLine);
    }
}
