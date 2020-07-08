package com.atlassian.bitbucket.plugins.slack.listener;

import com.atlassian.bitbucket.comment.AbstractCommentableVisitor;
import com.atlassian.bitbucket.comment.Comment;
import com.atlassian.bitbucket.comment.CommentService;
import com.atlassian.bitbucket.comment.CommentThread;
import com.atlassian.bitbucket.event.commit.CommitDiscussionCommentEvent;
import com.atlassian.bitbucket.event.pull.PullRequestEvent;
import com.atlassian.bitbucket.event.pull.PullRequestReviewersUpdatedEvent;
import com.atlassian.bitbucket.event.repository.RepositoryForkedEvent;
import com.atlassian.bitbucket.event.repository.RepositoryRefsChangedEvent;
import com.atlassian.bitbucket.event.task.TaskEvent;
import com.atlassian.bitbucket.plugins.slack.notification.NotificationPublisher;
import com.atlassian.bitbucket.plugins.slack.notification.PullRequestNotificationTypes;
import com.atlassian.bitbucket.plugins.slack.notification.RepositoryNotificationTypes;
import com.atlassian.bitbucket.plugins.slack.notification.TaskNotificationTypes;
import com.atlassian.bitbucket.plugins.slack.notification.configuration.PersonalNotificationService;
import com.atlassian.bitbucket.plugins.slack.notification.renderer.SlackNotificationRenderer;
import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.user.ApplicationUser;
import com.atlassian.event.api.EventListener;
import com.atlassian.plugins.slack.api.notification.Verbosity;
import com.atlassian.sal.api.message.I18nResolver;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest.ChatPostMessageRequestBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Supplier;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

/**
 * This class listens to Bitbucket events and tried to publish the respective notification.
 */
@Component
public class BitbucketNotificationEventListener {
    private final I18nResolver i18nResolver;

    private final SlackNotificationRenderer slackNotificationRenderer;
    private final NotificationPublisher notificationPublisher;
    private final CommentService commentService;
    private final PersonalNotificationService personalNotificationService;

    @Autowired
    public BitbucketNotificationEventListener(final I18nResolver i18nResolver,
                                              final SlackNotificationRenderer slackNotificationRenderer,
                                              final NotificationPublisher notificationPublisher,
                                              final CommentService commentService,
                                              final PersonalNotificationService personalNotificationService) {
        this.i18nResolver = i18nResolver;
        this.slackNotificationRenderer = slackNotificationRenderer;
        this.notificationPublisher = notificationPublisher;
        this.commentService = commentService;
        this.personalNotificationService = personalNotificationService;
    }

    @EventListener
    public void onEvent(final TaskEvent event) {
        final TaskNotificationTypes taskAction = TaskNotificationTypes.from(event);
        event.getTask().getAnchor().accept(commentStub -> {
            // need to refresh the comment object since in some cases it tries to lazy load its parent while the underlying session is closed
            commentService.getComment(commentStub.getId())
                    .map(Comment::getThread)
                    .map(CommentThread::getCommentable)
                    .ifPresent(commentable -> commentable.accept(new AbstractCommentableVisitor<Pair<Repository, Supplier<ChatPostMessageRequestBuilder>>>() {
                        @Override
                        public Pair<Repository, Supplier<ChatPostMessageRequestBuilder>> visit(@Nonnull final PullRequest pullRequest) {
                            notificationPublisher.findChannelsAndPublishNotificationsAsync(
                                    pullRequest.getToRef().getRepository(),
                                    taskAction.getKey(),
                                    () -> personalNotificationService.findNotificationsFor(
                                            event.getUser(), pullRequest, Collections.emptySet(), false),
                                    options -> ofNullable(slackNotificationRenderer.getPullRequestTaskMessage(pullRequest, event, taskAction)));
                            return null;
                        }
                    }));
            return null;
        });
    }

    @EventListener
    public void onEvent(final PullRequestEvent event) {
        PullRequestNotificationTypes.byEvent(event, i18nResolver).ifPresent(notificationType -> {
            final boolean isReviewersUpdate = event instanceof PullRequestReviewersUpdatedEvent;
            final boolean hasUserAddedOrRemovedHimself = isReviewersUpdate && hasUserAddedOrRemovedHimself((PullRequestReviewersUpdatedEvent) event);
            notificationPublisher.findChannelsAndPublishNotificationsAsync(
                    event.getPullRequest().getToRef().getRepository(),
                    notificationType.getKey(),
                    () -> personalNotificationService.findNotificationsFor(
                            event.getUser(),
                            event.getPullRequest(),
                            getUsersAddedToPullRequest(event),
                            false),
                    options -> {
                        if (isReviewersUpdate) {
                            if (hasUserAddedOrRemovedHimself || options.isPersonal()) {
                                return ofNullable(slackNotificationRenderer.getReviewersPullRequestMessage(
                                        (PullRequestReviewersUpdatedEvent) event,
                                        Verbosity.EXTENDED.equals(options.getVerbosity())));
                            }
                            return empty();
                        }
                        return ofNullable(slackNotificationRenderer.getPullRequestMessage(event));
                    });
        });
    }

    private Set<ApplicationUser> getUsersAddedToPullRequest(final PullRequestEvent event) {
        if (event instanceof PullRequestReviewersUpdatedEvent) {
            final PullRequestReviewersUpdatedEvent e = (PullRequestReviewersUpdatedEvent) event;
            return e.getAddedReviewers();
        }
        return Collections.emptySet();
    }

    private boolean hasUserAddedOrRemovedHimself(final PullRequestReviewersUpdatedEvent event) {
        final ApplicationUser actor = event.getUser();
        final Collection<ApplicationUser> addedReviewers = firstNonNull(event.getAddedReviewers(), emptyList());
        final Collection<ApplicationUser> removedReviewers = firstNonNull(event.getRemovedReviewers(), emptyList());
        final boolean isOneUserAdded = addedReviewers.size() == 1;
        final boolean isOneUserRemoved = removedReviewers.size() == 1;

        // produces event if, and only if, the user added or removed himself from the UI
        if (isOneUserAdded && !isOneUserRemoved) {
            return addedReviewers.iterator().next().equals(actor);
        } else if (!isOneUserAdded && isOneUserRemoved) {
            return removedReviewers.iterator().next().equals(actor);
        }
        return false;
    }

    @EventListener
    public void onEvent(final RepositoryRefsChangedEvent event) {
        RepositoryNotificationTypes.byEventClass(event.getClass()).ifPresent(notificationType ->
                event.getRefChanges().forEach(refChange ->
                        notificationPublisher.findChannelsAndPublishNotificationsAsync(
                                event.getRepository(),
                                notificationType.getKey(),
                                Collections::emptySet,
                                options -> ofNullable(slackNotificationRenderer.getPushMessage(
                                        event, refChange, options.getVerbosity())))));
    }

    @EventListener
    public void onEvent(final RepositoryForkedEvent event) {
        notificationPublisher.findChannelsAndPublishNotificationsAsync(
                event.getRepository().getOrigin(),
                RepositoryNotificationTypes.FORKED.getKey(),
                Collections::emptySet,
                options -> ofNullable(slackNotificationRenderer.getRepositoryForkedMessage(event)));
    }

    @EventListener
    public void onEvent(final CommitDiscussionCommentEvent event) {
        RepositoryNotificationTypes.byEventClass(event.getClass()).ifPresent(notificationType ->
                notificationPublisher.findChannelsAndPublishNotificationsAsync(
                        event.getRepository(),
                        notificationType.getKey(),
                        () -> personalNotificationService.findNotificationsFor(event.getUser(), event.getDiscussion()),
                        options -> ofNullable(slackNotificationRenderer.getCommitDiscussionMessage(event))));
    }
}
