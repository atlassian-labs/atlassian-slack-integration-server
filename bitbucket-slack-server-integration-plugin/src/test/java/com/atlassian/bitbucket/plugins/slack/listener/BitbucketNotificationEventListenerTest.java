package com.atlassian.bitbucket.plugins.slack.listener;

import com.atlassian.bitbucket.comment.Comment;
import com.atlassian.bitbucket.comment.CommentAction;
import com.atlassian.bitbucket.comment.CommentService;
import com.atlassian.bitbucket.comment.CommentSeverity;
import com.atlassian.bitbucket.comment.CommentState;
import com.atlassian.bitbucket.comment.CommentThread;
import com.atlassian.bitbucket.comment.Commentable;
import com.atlassian.bitbucket.comment.CommentableVisitor;
import com.atlassian.bitbucket.event.branch.BranchChangedEvent;
import com.atlassian.bitbucket.event.branch.BranchCreatedEvent;
import com.atlassian.bitbucket.event.branch.BranchDeletedEvent;
import com.atlassian.bitbucket.event.commit.CommitDiscussionCommentAddedEvent;
import com.atlassian.bitbucket.event.commit.CommitDiscussionCommentDeletedEvent;
import com.atlassian.bitbucket.event.commit.CommitDiscussionCommentEditedEvent;
import com.atlassian.bitbucket.event.commit.CommitDiscussionCommentEvent;
import com.atlassian.bitbucket.event.commit.CommitDiscussionCommentRepliedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestCommentAddedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestCommentDeletedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestCommentEditedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestCommentEvent;
import com.atlassian.bitbucket.event.pull.PullRequestCommentRepliedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestDeclinedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestDeletedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestEvent;
import com.atlassian.bitbucket.event.pull.PullRequestMergedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestOpenedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestParticipantApprovedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestParticipantReviewedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestParticipantUnapprovedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestReopenedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestReviewersUpdatedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestUpdatedEvent;
import com.atlassian.bitbucket.event.repository.RepositoryForkedEvent;
import com.atlassian.bitbucket.event.repository.RepositoryPushEvent;
import com.atlassian.bitbucket.event.tag.TagChangedEvent;
import com.atlassian.bitbucket.event.tag.TagCreatedEvent;
import com.atlassian.bitbucket.event.tag.TagDeletedEvent;
import com.atlassian.bitbucket.plugins.slack.model.NotificationRenderingOptions;
import com.atlassian.bitbucket.plugins.slack.notification.NotificationPublisher;
import com.atlassian.bitbucket.plugins.slack.notification.PullRequestNotificationTypes;
import com.atlassian.bitbucket.plugins.slack.notification.RepositoryNotificationTypes;
import com.atlassian.bitbucket.plugins.slack.notification.TaskNotificationTypes;
import com.atlassian.bitbucket.plugins.slack.notification.renderer.SlackNotificationRenderer;
import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.bitbucket.pull.PullRequestRef;
import com.atlassian.bitbucket.repository.RefChange;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.user.ApplicationUser;
import com.atlassian.plugins.slack.api.notification.Verbosity;
import com.atlassian.sal.api.message.I18nResolver;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest.ChatPostMessageRequestBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.atlassian.bitbucket.comment.CommentSeverity.NORMAL;
import static org.mockito.AdditionalAnswers.answer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BitbucketNotificationEventListenerTest {
    @Mock
    private SlackNotificationRenderer slackNotificationRenderer;
    @Mock
    private NotificationPublisher notificationPublisher;
    @Mock
    private I18nResolver i18nResolver;

    // supported pull request events
    @Mock
    private PullRequestOpenedEvent pullRequestOpenedEvent;
    @Mock
    private PullRequestUpdatedEvent pullRequestUpdatedEvent;
    @Mock
    private PullRequestDeclinedEvent pullRequestDeclinedEvent;
    @Mock
    private PullRequestReopenedEvent pullRequestReopenedEvent;
    @Mock
    private PullRequestMergedEvent pullRequestMergedEvent;
    @Mock
    private PullRequestDeletedEvent pullRequestDeletedEvent;
    @Mock
    private PullRequestParticipantApprovedEvent pullRequestParticipantApprovedEvent;
    @Mock
    private PullRequestParticipantUnapprovedEvent pullRequestParticipantUnapprovedEvent;
    @Mock
    private PullRequestParticipantReviewedEvent pullRequestParticipantReviewedEvent;
    @Mock
    private PullRequestCommentAddedEvent pullRequestCommentAddedEvent;
    @Mock
    private PullRequestCommentEditedEvent pullRequestCommentEditedEvent;
    @Mock
    private PullRequestCommentRepliedEvent pullRequestCommentRepliedEvent;
    @Mock
    private PullRequestCommentDeletedEvent pullRequestCommentDeletedEvent;
    @Mock
    private PullRequestReviewersUpdatedEvent pullRequestReviewersUpdatedEvent;

    // supported commit/repository events
    @Mock
    private RepositoryForkedEvent repositoryForkedEvent;
    @Mock
    private RepositoryPushEvent repositoryPushEvent;
    @Mock
    private TagCreatedEvent tagCreatedEvent;
    @Mock
    private TagDeletedEvent tagDeletedEvent;
    @Mock
    private BranchCreatedEvent branchCreatedEvent;
    @Mock
    private BranchDeletedEvent branchDeletedEvent;
    @Mock
    private CommitDiscussionCommentAddedEvent commitDiscussionCommentAddedEvent;
    @Mock
    private CommitDiscussionCommentEditedEvent commitDiscussionCommentEditedEvent;
    @Mock
    private CommitDiscussionCommentRepliedEvent commitDiscussionCommentRepliedEvent;
    @Mock
    private CommitDiscussionCommentDeletedEvent commitDiscussionCommentDeletedEvent;
    @Mock
    private Comment comment;
    @Mock
    private CommentThread commentThread;
    @Mock
    private Commentable commentable;
    @Mock
    private PullRequest pullRequest;
    @Mock
    private PullRequestRef pullRequestRef;
    @Mock
    private Repository repository;
    @Mock
    private Repository origin;
    @Mock
    private RefChange refChange;
    @Mock
    private ApplicationUser user;

    @Captor
    private ArgumentCaptor<Function<NotificationRenderingOptions, Optional<ChatPostMessageRequestBuilder>>> captor;

    @InjectMocks
    private BitbucketNotificationEventListener target;

    private NotificationRenderingOptions extendedOptions = new NotificationRenderingOptions(Verbosity.EXTENDED, false);

    private void testPullRequestBlockerCommentEvent(PullRequestCommentEvent event, TaskNotificationTypes type) {
        when(event.getComment()).thenReturn(comment);
        when(comment.getSeverity()).thenReturn(CommentSeverity.BLOCKER);
        when(comment.getThread()).thenReturn(commentThread);
        when(commentThread.getCommentable()).thenReturn(commentable);
        doAnswer(answer((CommentableVisitor v) -> v.visit(pullRequest))).when(commentable).accept(any());
        when(pullRequest.getToRef()).thenReturn(pullRequestRef);
        when(pullRequestRef.getRepository()).thenReturn(repository);

        target.onEvent(event);

        verify(notificationPublisher).findChannelsAndPublishNotificationsAsync(
                same(repository),
                eq(type.getKey()),
                any(),
                captor.capture());

        captor.getValue().apply(extendedOptions);
        verify(slackNotificationRenderer).getPullRequestTaskMessage(pullRequest, type, event.getComment(), event.getUser());
    }

    @Test
    void onEvent_pullRequestTaskCreated_shouldCallExpectedMethods() {
        when(pullRequestCommentAddedEvent.getCommentAction()).thenReturn(CommentAction.ADDED);
        testPullRequestBlockerCommentEvent(pullRequestCommentAddedEvent, TaskNotificationTypes.CREATED);
    }

    @Test
    void onEvent_pullRequestTaskReplied_shouldCallExpectedMethods() {
        when(pullRequestCommentAddedEvent.getCommentAction()).thenReturn(CommentAction.REPLIED);
        testPullRequestBlockerCommentEvent(pullRequestCommentAddedEvent, TaskNotificationTypes.CREATED);
    }

    @Test
    void onEvent_pullRequestTaskUpdated_shouldCallExpectedMethods() {
        when(pullRequestCommentEditedEvent.getCommentAction()).thenReturn(CommentAction.EDITED);
        testPullRequestBlockerCommentEvent(pullRequestCommentEditedEvent, TaskNotificationTypes.UPDATED);
    }

    @Test
    void onEvent_pullRequestTaskResolved_shouldCallExpectedMethods() {
        when(pullRequestCommentEditedEvent.getCommentAction()).thenReturn(CommentAction.EDITED);
        when(pullRequestCommentEditedEvent.getPreviousState()).thenReturn(CommentState.OPEN);
        when(comment.getState()).thenReturn(CommentState.RESOLVED);
        testPullRequestBlockerCommentEvent(pullRequestCommentEditedEvent, TaskNotificationTypes.RESOLVED);
    }

    @Test
    void onEvent_pullRequestTaskReopened_shouldCallExpectedMethods() {
        when(pullRequestCommentEditedEvent.getCommentAction()).thenReturn(CommentAction.EDITED);
        when(pullRequestCommentEditedEvent.getPreviousState()).thenReturn(CommentState.RESOLVED);
        when(comment.getState()).thenReturn(CommentState.OPEN);
        testPullRequestBlockerCommentEvent(pullRequestCommentEditedEvent, TaskNotificationTypes.REOPENED);
    }

    @Test
    void onEvent_pullRequestTaskDeleted_shouldCallExpectedMethods() {
        when(pullRequestCommentDeletedEvent.getCommentAction()).thenReturn(CommentAction.DELETED);
        when(pullRequestCommentDeletedEvent.getComment()).thenReturn(comment);
        testPullRequestBlockerCommentEvent(pullRequestCommentDeletedEvent, TaskNotificationTypes.DELETED);
    }

    void testPullRequestEvent(PullRequestEvent event, PullRequestNotificationTypes type) {
        testPullRequestEvent(event, type, "PR title", false);
    }

    void testPullRequestEvent(PullRequestEvent event, PullRequestNotificationTypes type, boolean reviewers) {
        testPullRequestEvent(event, type, "PR title", reviewers);
    }

    void testPullRequestEvent(PullRequestEvent event, PullRequestNotificationTypes type, String prTitle, boolean reviewers) {
        when(event.getPullRequest()).thenReturn(pullRequest);
        when(pullRequest.getTitle()).thenReturn(prTitle);
        when(pullRequest.getToRef()).thenReturn(pullRequestRef);
        when(pullRequestRef.getRepository()).thenReturn(repository);
        when(i18nResolver.getText(anyString())).thenAnswer(answer(k -> k));

        target.onEvent(event);

        verify(notificationPublisher).findChannelsAndPublishNotificationsAsync(
                same(repository),
                eq(type.getKey()),
                any(),
                captor.capture());

        captor.getValue().apply(extendedOptions);
        if (reviewers) {
            verify(slackNotificationRenderer).getReviewersPullRequestMessage((PullRequestReviewersUpdatedEvent) event, true);
        } else {
            verify(slackNotificationRenderer).getPullRequestMessage(event);
        }
    }

    @Test
    void onEvent_pullRequestOpenedEvent_shouldCallExpectedMethods() {
        testPullRequestEvent(pullRequestOpenedEvent, PullRequestNotificationTypes.OPENED);
    }

    @Test
    void onEvent_pullRequestUpdatedEvent_shouldCallExpectedMethods() {
        testPullRequestEvent(pullRequestUpdatedEvent, PullRequestNotificationTypes.UPDATED);
    }

    @Test
    void onEvent_pullRequestDeclinedEvent_shouldCallExpectedMethods() {
        testPullRequestEvent(pullRequestDeclinedEvent, PullRequestNotificationTypes.DECLINED);
    }

    @Test
    void onEvent_pullRequestReopenedEvent_shouldCallExpectedMethods() {
        testPullRequestEvent(pullRequestReopenedEvent, PullRequestNotificationTypes.REOPENED);
    }

    @Test
    void onEvent_pullRequestMergedEvent_shouldCallExpectedMethods() {
        testPullRequestEvent(pullRequestMergedEvent, PullRequestNotificationTypes.MERGED);
    }

    @Test
    void onEvent_pullRequestDeletedEvent_shouldCallExpectedMethods() {
        testPullRequestEvent(pullRequestDeletedEvent, PullRequestNotificationTypes.DELETED);
    }

    @Test
    void onEvent_pullRequestParticipantApprovedEvent_shouldCallExpectedMethods() {
        testPullRequestEvent(pullRequestParticipantApprovedEvent, PullRequestNotificationTypes.APPROVED);
    }

    @Test
    void onEvent_pullRequestParticipantUnapprovedEvent_shouldCallExpectedMethods() {
        testPullRequestEvent(pullRequestParticipantUnapprovedEvent, PullRequestNotificationTypes.UNAPPROVED);
    }

    @Test
    void onEvent_pullRequestParticipantReviewedEvent_shouldCallExpectedMethods() {
        testPullRequestEvent(pullRequestParticipantReviewedEvent, PullRequestNotificationTypes.REVIEWED);
    }

    @Test
    void onEvent_pullRequestCommentAddedEvent_shouldCallExpectedMethods() {
        when(pullRequestCommentAddedEvent.getComment()).thenReturn(comment);
        when(comment.getSeverity()).thenReturn(NORMAL);
        testPullRequestEvent(pullRequestCommentAddedEvent, PullRequestNotificationTypes.COMMENT_ADDED);
    }

    @Test
    void onEvent_pullRequestCommentEditedEvent_shouldCallExpectedMethods() {
        when(comment.getSeverity()).thenReturn(NORMAL);
        when(pullRequestCommentEditedEvent.getComment()).thenReturn(comment);
        testPullRequestEvent(pullRequestCommentEditedEvent, PullRequestNotificationTypes.COMMENT_EDITED);
    }

    @Test
    void onEvent_pullRequestCommentRepliedEvent_shouldCallExpectedMethods() {
        when(comment.getSeverity()).thenReturn(NORMAL);
        when(pullRequestCommentRepliedEvent.getComment()).thenReturn(comment);
        testPullRequestEvent(pullRequestCommentRepliedEvent, PullRequestNotificationTypes.COMMENT_REPLIED);
    }

    @Test
    void onEvent_pullRequestCommentDeletedEvent_shouldCallExpectedMethods() {
        when(comment.getSeverity()).thenReturn(NORMAL);
        when(pullRequestCommentDeletedEvent.getComment()).thenReturn(comment);
        testPullRequestEvent(pullRequestCommentDeletedEvent, PullRequestNotificationTypes.COMMENT_DELETED);
    }

    @Test
    void onEvent_pullRequestReviewersUpdatedEvent_shouldCallExpectedMethods() {
        when(pullRequestReviewersUpdatedEvent.getAddedReviewers()).thenReturn(Collections.singleton(user));
        when(pullRequestReviewersUpdatedEvent.getUser()).thenReturn(user);
        testPullRequestEvent(pullRequestReviewersUpdatedEvent, PullRequestNotificationTypes.REVIEWERS_UPDATED, true);
    }

    @Test
    void onEvent_pullRequestAutoMergeFailedEvent_shouldCallExpectedMethods() {
        testPullRequestEvent(pullRequestOpenedEvent, PullRequestNotificationTypes.AUTO_MERGE_FAILED,
                "bitbucket.automerge.conflicted.pull-request.title PR", false);
    }

    @Test
    void onEvent_pullRequestAutoMergeRestoredEvent_shouldCallExpectedMethods() {
        testPullRequestEvent(pullRequestMergedEvent, PullRequestNotificationTypes.AUTO_MERGE_RESOLVED,
                "bitbucket.automerge.conflicted.pull-request.title PR", false);
    }

    void testCommitEvent(CommitDiscussionCommentEvent event, RepositoryNotificationTypes type) {
        when(event.getRepository()).thenReturn(repository);

        target.onEvent(event);

        verify(notificationPublisher).findChannelsAndPublishNotificationsAsync(
                same(repository),
                eq(type.getKey()),
                any(),
                captor.capture());

        captor.getValue().apply(extendedOptions);
        verify(slackNotificationRenderer).getCommitDiscussionMessage(event);
    }

    @Test
    void onEvent_commitDiscussionCommentAddedEvent_shouldCallExpectedMethods() {
        testCommitEvent(commitDiscussionCommentAddedEvent, RepositoryNotificationTypes.COMMENT_ADDED);
    }

    @Test
    void onEvent_commitDiscussionCommentEditedEvent_shouldCallExpectedMethods() {
        testCommitEvent(commitDiscussionCommentEditedEvent, RepositoryNotificationTypes.COMMENT_EDITED);
    }

    @Test
    void onEvent_commitDiscussionCommentRepliedEvent_shouldCallExpectedMethods() {
        testCommitEvent(commitDiscussionCommentRepliedEvent, RepositoryNotificationTypes.COMMENT_REPLIED);
    }

    @Test
    void onEvent_commitDiscussionCommentDeletedEvent_shouldCallExpectedMethods() {
        testCommitEvent(commitDiscussionCommentDeletedEvent, RepositoryNotificationTypes.COMMENT_DELETED);
    }

    @Test
    void onEvent_repositoryForkedEvent_shouldCallExpectedMethods() {
        when(repositoryForkedEvent.getRepository()).thenReturn(repository);
        when(repository.getOrigin()).thenReturn(origin);

        target.onEvent(repositoryForkedEvent);

        verify(notificationPublisher).findChannelsAndPublishNotificationsAsync(
                same(origin),
                eq(RepositoryNotificationTypes.FORKED.getKey()),
                any(),
                captor.capture());

        captor.getValue().apply(extendedOptions);
        verify(slackNotificationRenderer).getRepositoryForkedMessage(repositoryForkedEvent);
    }

    @Test
    void onEvent_repositoryPushEvent_shouldCallExpectedMethods() {
        when(repositoryPushEvent.getRepository()).thenReturn(repository);
        when(repositoryPushEvent.getRefChanges()).thenReturn(Collections.singleton(refChange));

        target.onEvent(repositoryPushEvent);

        verify(notificationPublisher).findChannelsAndPublishNotificationsAsync(
                same(repository),
                eq(RepositoryNotificationTypes.PUSHED.getKey()),
                any(),
                captor.capture());

        captor.getValue().apply(extendedOptions);
        verify(slackNotificationRenderer).getPushMessage(repositoryPushEvent, refChange, Verbosity.EXTENDED);
    }

    void testPushEvent(TagChangedEvent event, RepositoryNotificationTypes type) {
        when(event.getRepository()).thenReturn(repository);
        when(event.getRefChanges()).thenReturn(Collections.singleton(refChange));

        target.onEvent(event);

        verify(notificationPublisher).findChannelsAndPublishNotificationsAsync(
                same(repository),
                eq(type.getKey()),
                any(),
                captor.capture());

        captor.getValue().apply(extendedOptions);
        verify(slackNotificationRenderer).getPushMessage(event, refChange, Verbosity.EXTENDED);
    }

    @Test
    void onEvent_tagCreatedEvent_shouldCallExpectedMethods() {
        testPushEvent(tagCreatedEvent, RepositoryNotificationTypes.TAG_CREATED);
    }

    @Test
    void onEvent_tagDeletedEvent_shouldCallExpectedMethods() {
        testPushEvent(tagDeletedEvent, RepositoryNotificationTypes.TAG_DELETED);
    }

    void testPushEvent(BranchChangedEvent event, RepositoryNotificationTypes type) {
        when(event.getRepository()).thenReturn(repository);
        when(event.getRefChanges()).thenReturn(Collections.singleton(refChange));

        target.onEvent(event);

        verify(notificationPublisher).findChannelsAndPublishNotificationsAsync(
                same(repository),
                eq(type.getKey()),
                any(),
                captor.capture());

        captor.getValue().apply(extendedOptions);
        verify(slackNotificationRenderer).getPushMessage(event, refChange, Verbosity.EXTENDED);
    }

    @Test
    void onEvent_branchCreatedEvent_shouldCallExpectedMethods() {
        testPushEvent(branchCreatedEvent, RepositoryNotificationTypes.BRANCH_CREATED);
    }

    @Test
    void onEvent_branchDeletedEvent_shouldCallExpectedMethods() {
        testPushEvent(branchDeletedEvent, RepositoryNotificationTypes.BRANCH_DELETED);
    }
}
