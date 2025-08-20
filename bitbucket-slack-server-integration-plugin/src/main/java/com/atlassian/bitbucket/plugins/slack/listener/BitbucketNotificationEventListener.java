package com.atlassian.bitbucket.plugins.slack.listener;

import com.atlassian.bitbucket.comment.AbstractCommentableVisitor;
import com.atlassian.bitbucket.event.commit.CommitDiscussionCommentEvent;
import com.atlassian.bitbucket.event.content.FileEditedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestCommentEvent;
import com.atlassian.bitbucket.event.pull.PullRequestEvent;
import com.atlassian.bitbucket.event.pull.PullRequestOpenedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestRescopedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestReviewersUpdatedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestUpdatedEvent;
import com.atlassian.bitbucket.event.repository.RepositoryForkedEvent;
import com.atlassian.bitbucket.event.repository.RepositoryRefsChangedEvent;
import com.atlassian.bitbucket.plugins.slack.notification.NotificationPublisher;
import com.atlassian.bitbucket.plugins.slack.notification.PullRequestNotificationTypes;
import com.atlassian.bitbucket.plugins.slack.notification.RepositoryNotificationTypes;
import com.atlassian.bitbucket.plugins.slack.notification.TaskNotificationTypes;
import com.atlassian.bitbucket.plugins.slack.notification.configuration.PersonalNotificationService;
import com.atlassian.bitbucket.plugins.slack.notification.renderer.SlackNotificationRenderer;
import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.bitbucket.pull.PullRequestParticipant;
import com.atlassian.bitbucket.repository.Ref;
import com.atlassian.bitbucket.repository.RefChange;
import com.atlassian.bitbucket.repository.RefChangeType;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.repository.StandardRefType;
import com.atlassian.bitbucket.user.ApplicationUser;
import com.atlassian.event.api.EventListener;
import com.atlassian.plugins.slack.api.notification.Verbosity;
import com.atlassian.sal.api.message.I18nResolver;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest.ChatPostMessageRequestBuilder;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.atlassian.bitbucket.comment.CommentSeverity.BLOCKER;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

/**
 * This class listens to Bitbucket events and tried to publish the respective notification.
 */
@Slf4j
@Component
public class BitbucketNotificationEventListener {
    private final I18nResolver i18nResolver;

    private final SlackNotificationRenderer slackNotificationRenderer;
    private final NotificationPublisher notificationPublisher;
    private final PersonalNotificationService personalNotificationService;

    @Autowired
    public BitbucketNotificationEventListener(final I18nResolver i18nResolver,
                                              final SlackNotificationRenderer slackNotificationRenderer,
                                              final NotificationPublisher notificationPublisher,
                                              final PersonalNotificationService personalNotificationService) {
        this.i18nResolver = i18nResolver;
        this.slackNotificationRenderer = slackNotificationRenderer;
        this.notificationPublisher = notificationPublisher;
        this.personalNotificationService = personalNotificationService;
    }

    @EventListener
    public void onEvent(final PullRequestEvent event) {
        if (event instanceof PullRequestCommentEvent) {
            PullRequestCommentEvent commentEvent = (PullRequestCommentEvent) event;
            if (commentEvent.getComment().getSeverity() == BLOCKER) { //BLOCKER severity represents a PR task
                onEvent(commentEvent);
                return;
            }
        }
        if (event instanceof PullRequestRescopedEvent) {
            PullRequestRescopedEvent rescopedEvent = (PullRequestRescopedEvent) event;
            // no changes for target PR
            if (!Optional.ofNullable(rescopedEvent.getAddedCommits()).filter(commits -> commits.getTotal() > 0).isPresent() &&
                    !Optional.ofNullable(rescopedEvent.getRemovedCommits()).filter(commits -> commits.getTotal() > 0).isPresent()) {
                return;
            }
        }

        if (event instanceof PullRequestUpdatedEvent) {
            PullRequestUpdatedEvent updatedEvent = (PullRequestUpdatedEvent) event;
            PullRequest pullRequest = updatedEvent.getPullRequest();
            // no changes for source PR
            if (StringUtils.equals(updatedEvent.getPreviousDescription(), pullRequest.getDescription()) &&
                    StringUtils.equals(updatedEvent.getPreviousTitle(), pullRequest.getTitle()) &&
                    equalsToBranch(updatedEvent)) {
                return;
            }
        }

        Optional<PullRequestNotificationTypes> type = PullRequestNotificationTypes.byEvent(event, i18nResolver);
        log.debug("PR event class {} is mapped to type {}", event.getClass().getName(), type.orElse(null));

        type.ifPresent(notificationType -> {
            final boolean isReviewersUpdate = event instanceof PullRequestReviewersUpdatedEvent;
            final boolean hasUserAddedOrRemovedHimself = isReviewersUpdate && hasUserAddedOrRemovedHimself((PullRequestReviewersUpdatedEvent) event);
            final Set<ApplicationUser> addedReviewers = getAddedReviewers(event);
            notificationPublisher.findChannelsAndPublishNotificationsAsync(
                    event.getPullRequest().getToRef().getRepository(),
                    notificationType.getKey(),
                    () -> personalNotificationService.findNotificationsFor(
                            event.getUser(),
                            event.getPullRequest(),
                            addedReviewers),
                    options -> {
                        if (isReviewersUpdate) {
                            if (hasUserAddedOrRemovedHimself) {
                                return ofNullable(slackNotificationRenderer.getReviewersPullRequestMessage(
                                        event.getPullRequest(),
                                        event.getUser(),
                                        addedReviewers.contains(event.getUser()),
                                        Verbosity.EXTENDED.equals(options.getVerbosity())));
                            } else if (options.isPersonal()) {
                                ApplicationUser affectedUser = ObjectUtils.firstNonNull(options.getApplicationUser(), event.getUser());
                                return ofNullable(slackNotificationRenderer.getReviewersPullRequestMessage(
                                        event.getPullRequest(),
                                        affectedUser,
                                        addedReviewers.contains(affectedUser),
                                        Verbosity.EXTENDED.equals(options.getVerbosity())));
                            }
                            return empty();
                        }
                        return ofNullable(slackNotificationRenderer.getPullRequestMessage(event));
                    });
        });
    }

    private void onEvent(final PullRequestCommentEvent taskEvent) {
        final TaskNotificationTypes taskAction = TaskNotificationTypes.from(taskEvent);
        taskEvent.getComment()
                .getThread()
                .getCommentable()
                .accept(new AbstractCommentableVisitor<Pair<Repository, Supplier<ChatPostMessageRequestBuilder>>>() {
                    @Override
                    public Pair<Repository, Supplier<ChatPostMessageRequestBuilder>> visit(@Nonnull final PullRequest pullRequest) {
                        notificationPublisher.findChannelsAndPublishNotificationsAsync(
                                pullRequest.getToRef().getRepository(),
                                taskAction.getKey(),
                                () -> personalNotificationService.findNotificationsFor(taskEvent.getUser(),
                                        pullRequest, Collections.emptySet()),
                                options -> ofNullable(slackNotificationRenderer.getPullRequestTaskMessage(pullRequest,
                                        taskAction, taskEvent.getComment(), taskEvent.getUser())));
                        return null;
                    }
                });
    }

    private Set<ApplicationUser> getAddedReviewers(final PullRequestEvent event) {
        Set<ApplicationUser> addedReviewers = Collections.emptySet();
        if (event instanceof PullRequestReviewersUpdatedEvent) {
            final PullRequestReviewersUpdatedEvent e = (PullRequestReviewersUpdatedEvent) event;
            addedReviewers = e.getAddedReviewers();
        } else if (event instanceof PullRequestOpenedEvent) {
            addedReviewers = event.getPullRequest().getReviewers().stream()
                    .map(PullRequestParticipant::getUser)
                    .collect(Collectors.toSet());
        }
        return addedReviewers;
    }

    private boolean hasUserAddedOrRemovedHimself(final PullRequestReviewersUpdatedEvent event) {
        final ApplicationUser actor = event.getUser();
        final Collection<ApplicationUser> addedReviewers = firstNonNull(event.getAddedReviewers(), emptyList());
        final Collection<ApplicationUser> removedReviewers = firstNonNull(event.getRemovedReviewers(), emptyList());
        final boolean isOneUserAdded = addedReviewers.size() == 1;
        final boolean isOneUserRemoved = removedReviewers.size() == 1;

        // produces event if, and only if, the user added or removed himself from the UI
        boolean result = false;
        if (isOneUserAdded && !isOneUserRemoved) {
            result = addedReviewers.iterator().next().equals(actor);
        } else if (!isOneUserAdded && isOneUserRemoved) {
            result = removedReviewers.iterator().next().equals(actor);
        }
        return result;
    }

    private boolean equalsToBranch(final PullRequestUpdatedEvent updatedEvent) {
        Ref previousToRef = updatedEvent.getPreviousToBranch();
        if (previousToRef == null) {
            return true;
        }
        return previousToRef.getId().equals(updatedEvent.getPreviousToBranch().getId());
    }

    @EventListener
    public void onEvent(final RepositoryRefsChangedEvent event) {
        RepositoryNotificationTypes.byEventClass(event.getClass()).ifPresent(notificationType ->
                event.getRefChanges().forEach(refChange -> {
                    // RepositoryRefsChangedEvent event is also triggered when branch/tag is deleted via push
                    // replacing it here allows to respect 'branch/tag deleted' option in subscription settings
                    RepositoryNotificationTypes correctedType = correctNotificationType(notificationType, refChange);

                    notificationPublisher.findChannelsAndPublishNotificationsAsync(
                            event.getRepository(),
                            correctedType.getKey(),
                            Collections::emptySet,
                            options -> correctedType == RepositoryNotificationTypes.FILE_EDITED
                                    ? ofNullable(slackNotificationRenderer.getFileEditedMessage(
                                    (FileEditedEvent) event, refChange, options.getVerbosity()))
                                    : ofNullable(slackNotificationRenderer.getPushMessage(event, refChange,
                                    options.getVerbosity())));
                }));
    }

    private RepositoryNotificationTypes correctNotificationType(final RepositoryNotificationTypes currentType,
                                                                final RefChange refChange) {
        RepositoryNotificationTypes correctedType = currentType;
        if (currentType == RepositoryNotificationTypes.PUSHED
                && refChange.getType() == RefChangeType.DELETE) {
            correctedType = refChange.getRef().getType() == StandardRefType.BRANCH
                    ? RepositoryNotificationTypes.BRANCH_DELETED
                    : RepositoryNotificationTypes.TAG_DELETED;
        }

        return correctedType;
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
