package com.atlassian.jira.plugins.slack.service.notification.impl;

import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.comments.Comment;
import com.atlassian.jira.plugins.slack.model.EventMatcherType;
import com.atlassian.jira.plugins.slack.model.SlackNotification;
import com.atlassian.jira.plugins.slack.model.event.JiraIssueEvent;
import com.atlassian.jira.plugins.slack.model.event.PluginEvent;
import com.atlassian.jira.plugins.slack.service.notification.AttachmentHelper;
import com.atlassian.jira.plugins.slack.service.notification.MessageRendererException;
import com.atlassian.jira.plugins.slack.service.notification.NotificationInfo;
import com.atlassian.jira.plugins.slack.util.changelog.ChangeLogExtractor;
import com.atlassian.jira.plugins.slack.util.changelog.ChangeLogItem;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.plugins.slack.api.notification.Verbosity;
import com.atlassian.sal.api.message.I18nResolver;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest.ChatPostMessageRequestBuilder;
import com.github.seratch.jslack.api.model.Attachment;
import com.github.seratch.jslack.api.model.Field;
import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;

@Component("jiraIssueEventRenderer")
public class JiraIssueEventRenderer extends AbstractEventRenderer<JiraIssueEvent> {
    private static final int FIELD_VALUE_MAX_LENGTH = 1000;

    private final I18nResolver i18nResolver;
    private final ChangeLogExtractor changeLogExtractor;
    private final AttachmentHelper attachmentHelper;
    private final UserManager userManager;

    @Autowired
    public JiraIssueEventRenderer(I18nResolver i18nResolver,
                                  ChangeLogExtractor changeLogExtractor,
                                  AttachmentHelper attachmentHelper,
                                  @Qualifier("jiraUserManager") UserManager userManager) {
        this.i18nResolver = i18nResolver;
        this.changeLogExtractor = changeLogExtractor;
        this.attachmentHelper = attachmentHelper;
        this.userManager = userManager;
    }

    @Override
    protected List<SlackNotification> doRender(final JiraIssueEvent pluginEvent,
                                               final List<NotificationInfo> notificationInfos) {
        return notificationInfos.stream()
                .map(notificationInfo -> renderNotification(pluginEvent, notificationInfo)
                        .map(message -> message
                                .channel(notificationInfo.getChannelId())
                                .threadTs(notificationInfo.getThreadTimestamp())
                                .build())
                        .map(message -> new SlackNotification(notificationInfo, message)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    @Override
    public boolean canRender(PluginEvent pluginEvent) {
        return (pluginEvent instanceof JiraIssueEvent);
    }

    private Optional<ChatPostMessageRequestBuilder> renderNotification(final JiraIssueEvent jiraIssueEvent,
                                                                       final NotificationInfo notificationInfo) {
        try {
            return renderStandardNotification(jiraIssueEvent, notificationInfo);
        } catch (IOException e) {
            throw new MessageRendererException(e);
        }
    }

    private Optional<ChatPostMessageRequestBuilder> renderStandardNotification(
            final JiraIssueEvent jiraIssueEvent,
            final NotificationInfo notificationInfo
    ) throws IOException {
        final Issue issue = jiraIssueEvent.getIssueEvent().getIssue();
        final String userLink = attachmentHelper.userLink(jiraIssueEvent.getIssueEvent().getUser());
        final String issueType = Objects.requireNonNull(issue.getIssueType()).getName();
        final String issueTypeArticle = issueType.toLowerCase().matches("[aeiou].*")
                ? i18nResolver.getText("jira.slack.card.notification.issue.type.article.an")
                : i18nResolver.getText("jira.slack.card.notification.issue.type.article.a");
        final Verbosity verbosity = notificationInfo.getVerbosity();
        final boolean isExtendedVerbosity = Verbosity.EXTENDED == verbosity;

        return Optional.ofNullable(jiraIssueEvent.getEventMatcher().accept(
                new EventMatcherType.Visitor<ChatPostMessageRequestBuilder, IOException>() {
                    @Override
                    public ChatPostMessageRequestBuilder visitCreated() {
                        final String text = i18nResolver.getText(
                                "jira.slack.card.notification.event.created",
                                userLink,
                                issueTypeArticle,
                                issueType,
                                isExtendedVerbosity ? "" : attachmentHelper.issueLink(issue)
                        );
                        return ChatPostMessageRequest.builder()
                                .text(text)
                                .mrkdwn(true)
                                .attachments(buildAttachments(isExtendedVerbosity,
                                        () -> attachmentHelper.buildIssueAttachment(null, issue, null)));
                    }

                    @Override
                    public ChatPostMessageRequestBuilder visitUpdated() {
                        final String text = i18nResolver.getText(
                                "jira.slack.card.notification.event.updated",
                                userLink,
                                issueTypeArticle,
                                issueType,
                                isExtendedVerbosity ? "" : attachmentHelper.issueLink(issue)
                        );
                        return ChatPostMessageRequest.builder()
                                .text(text)
                                .mrkdwn(true)
                                .attachments(buildAttachments(isExtendedVerbosity,
                                        () -> attachmentHelper.buildIssueAttachment(null, issue, createUpdateFields(jiraIssueEvent))));
                    }

                    @Override
                    public ChatPostMessageRequestBuilder visitTransitioned() {
                        final Optional<String> oldStatusOptional =
                                getChanges(
                                        jiraIssueEvent.getIssueEvent(),
                                        Collections.singleton(ChangeLogExtractor.STATUS_FIELD_NAME)
                                ).stream()
                                        .map(ChangeLogItem::getOldText)
                                        .filter(StringUtils::isNotBlank)
                                        .findFirst();

                        final String text = oldStatusOptional
                                .map(oldStatus -> i18nResolver.getText(
                                        "jira.slack.card.notification.event.transitioned.with.previous",
                                        userLink,
                                        issueTypeArticle,
                                        issueType,
                                        isExtendedVerbosity ? "" : attachmentHelper.issueLink(issue),
                                        oldStatus,
                                        issue.getStatus().getName()))
                                .orElseGet(() -> i18nResolver.getText(
                                        "jira.slack.card.notification.event.transitioned",
                                        userLink,
                                        issueTypeArticle,
                                        issueType,
                                        isExtendedVerbosity ? "" : attachmentHelper.issueLink(issue),
                                        issue.getStatus().getName()));
                        return ChatPostMessageRequest.builder()
                                .text(text)
                                .mrkdwn(true)
                                .attachments(buildAttachments(isExtendedVerbosity,
                                        () -> attachmentHelper.buildIssueAttachment(null, issue, null)));
                    }

                    @Override
                    public ChatPostMessageRequestBuilder visitCommented() {
                        final Comment comment = jiraIssueEvent.getIssueEvent().getComment();
                        String text = i18nResolver.getText(
                                isCommentUpdated(comment)
                                        ? "jira.slack.card.notification.event.comment.updated"
                                        : "jira.slack.card.notification.event.commented",
                                userLink,
                                issueTypeArticle,
                                issueType,
                                isExtendedVerbosity ? "" : attachmentHelper.issueLink(issue)
                        );
                        return ChatPostMessageRequest.builder()
                                .text(isExtendedVerbosity ? text : text + String.format(": _%s_", comment.getBody()))
                                .mrkdwn(true)
                                .attachments(buildAttachments(isExtendedVerbosity,
                                        () -> attachmentHelper.buildCommentAttachment(null, issue, comment)));
                    }

                    @Override
                    public ChatPostMessageRequestBuilder visitAssignmentChanged() {
                        final Optional<ChangeLogItem> changes =
                                getChanges(
                                        jiraIssueEvent.getIssueEvent(),
                                        Collections.singleton(ChangeLogExtractor.ASSIGNEE_FIELD_NAME)
                                ).stream().findAny();
                        if (!changes.isPresent()) {
                            return null;
                        }
                        final String text = getAssigneeText(
                                changes.get().getOldValue(),
                                changes.get().getNewValue(),
                                userLink,
                                issueTypeArticle,
                                issueType,
                                isExtendedVerbosity ? "" : attachmentHelper.issueLink(issue)
                        );
                        return ChatPostMessageRequest.builder()
                                .text(text)
                                .mrkdwn(true)
                                .attachments(buildAttachments(isExtendedVerbosity,
                                        () -> attachmentHelper.buildIssueAttachment(null, issue, null)));
                    }
                }));
    }

    private String getAssigneeText(String previousUserName,
                                   String assigneeUserName,
                                   String userLink,
                                   String issueTypeArticle,
                                   String issueType,
                                   String issueLink) {
        if (StringUtils.isNotBlank(assigneeUserName)) {
            // assigned
            if (StringUtils.isNotBlank(previousUserName)) {
                return i18nResolver.getText(
                        "jira.slack.card.notification.event.assigned.from",
                        userLink,
                        issueTypeArticle,
                        issueType,
                        issueLink,
                        attachmentHelper.userLink(userManager.getUserByKey(previousUserName)),
                        attachmentHelper.userLink(userManager.getUserByKey(assigneeUserName)));
            } else {
                return i18nResolver.getText(
                        "jira.slack.card.notification.event.assigned",
                        userLink,
                        issueTypeArticle,
                        issueType,
                        issueLink,
                        attachmentHelper.userLink(userManager.getUserByKey(assigneeUserName)));
            }
        } else {
            // unassigned
            if (StringUtils.isNotBlank(previousUserName)) {
                return i18nResolver.getText(
                        "jira.slack.card.notification.event.unassigned.from",
                        userLink,
                        issueTypeArticle,
                        issueType,
                        issueLink,
                        attachmentHelper.userLink(userManager.getUserByKey(previousUserName)));
            } else {
                return i18nResolver.getText(
                        "jira.slack.card.notification.event.unassigned",
                        userLink,
                        issueTypeArticle,
                        issueType,
                        issueLink);
            }
        }
    }

    private boolean isCommentUpdated(final Comment comment) {
        Date commentCreated = comment.getCreated();
        Date commentUpdated = comment.getUpdated();
        // This is the best thing we have to tell a new comment from an update to an existing one.
        return (commentUpdated != null && !commentUpdated.equals(commentCreated));
    }

    private List<Field> createUpdateFields(final JiraIssueEvent jiraIssueEvent) {
        return getChanges(jiraIssueEvent.getIssueEvent(), Collections.emptySet()).stream()
                .filter(c -> !c.getField().equals(ChangeLogExtractor.ASSIGNEE_FIELD_NAME))
                .map(c -> Field.builder()
                        .title(c.getFieldName())
                        .value(c.getNewText())
                        .valueShortEnough(c.getNewText().length() <= 15)
                        .build())
                .collect(Collectors.toList());
    }

    private List<ChangeLogItem> getChanges(final IssueEvent issueEvent, final Set<String> fields) {
        return changeLogExtractor.getChanges(issueEvent, FIELD_VALUE_MAX_LENGTH).stream()
                .filter(changeLogItem -> !Strings.isNullOrEmpty(changeLogItem.getNewText()) &&
                        (fields.isEmpty() || fields.contains(changeLogItem.getField())))
                .collect(Collectors.toList());
    }

    private List<Attachment> buildAttachments(final boolean isExtendedVerbosity,
                                              final Supplier<Attachment> attachmentSupplier) {
        return isExtendedVerbosity ? singletonList(attachmentSupplier.get()) : null;
    }
}
