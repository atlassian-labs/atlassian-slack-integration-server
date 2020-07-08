package com.atlassian.bitbucket.plugins.slack.notification;

import com.atlassian.bitbucket.event.pull.PullRequestCommentAddedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestCommentDeletedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestCommentEditedEvent;
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
import com.atlassian.sal.api.message.I18nResolver;
import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.startsWith;

public enum PullRequestNotificationTypes {
    OPENED("PRCreated"),
    UPDATED("PRParticipantUpdated"),
    DECLINED("PRDeclined"),
    REOPENED("PRReopened"),
    MERGED("PRMerged"),
    DELETED("PRDeleted"),
    APPROVED("PRParticipantApproved"),
    UNAPPROVED("PRParticipantUnapproved"),
    REVIEWED("PRParticipantReviewed"),
    COMMENT_ADDED("PRComment"),
    COMMENT_EDITED("PRCommentEdited"),
    COMMENT_REPLIED("PRCommentReplied"),
    COMMENT_DELETED("PRCommentDeleted"),
    REVIEWERS_UPDATED("PRReviewersUpdated"),
    AUTO_MERGE_FAILED("PRAutoMergeFailed"),
    AUTO_MERGE_RESOLVED("PRAutoMergeResolved");

    private static final Map<Class<? extends PullRequestEvent>, PullRequestNotificationTypes> byEventClass =
            ImmutableMap.<Class<? extends PullRequestEvent>, PullRequestNotificationTypes>builder()
                    .put(PullRequestOpenedEvent.class, PullRequestNotificationTypes.OPENED)
                    .put(PullRequestUpdatedEvent.class, PullRequestNotificationTypes.UPDATED)
                    .put(PullRequestDeclinedEvent.class, PullRequestNotificationTypes.DECLINED)
                    .put(PullRequestReopenedEvent.class, PullRequestNotificationTypes.REOPENED)
                    .put(PullRequestMergedEvent.class, PullRequestNotificationTypes.MERGED)
                    .put(PullRequestDeletedEvent.class, PullRequestNotificationTypes.DELETED)
                    .put(PullRequestParticipantApprovedEvent.class, PullRequestNotificationTypes.APPROVED)
                    .put(PullRequestParticipantUnapprovedEvent.class, PullRequestNotificationTypes.UNAPPROVED)
                    .put(PullRequestParticipantReviewedEvent.class, PullRequestNotificationTypes.REVIEWED)
                    .put(PullRequestCommentAddedEvent.class, PullRequestNotificationTypes.COMMENT_ADDED)
                    .put(PullRequestCommentEditedEvent.class, PullRequestNotificationTypes.COMMENT_EDITED)
                    .put(PullRequestCommentRepliedEvent.class, PullRequestNotificationTypes.COMMENT_REPLIED)
                    .put(PullRequestCommentDeletedEvent.class, PullRequestNotificationTypes.COMMENT_DELETED)
                    .put(PullRequestReviewersUpdatedEvent.class, PullRequestNotificationTypes.REVIEWERS_UPDATED)
                    .build();
    private static final Map<PullRequestNotificationTypes, PullRequestNotificationTypes> autoMergeTypes =
            ImmutableMap.<PullRequestNotificationTypes, PullRequestNotificationTypes> builder()
                    .put(OPENED, AUTO_MERGE_FAILED)
                    .put(MERGED, AUTO_MERGE_RESOLVED)
                    .build();

    private final String key;

    PullRequestNotificationTypes(final String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public static Optional<PullRequestNotificationTypes> byEvent(final PullRequestEvent event,
                                                                 final I18nResolver i18nResolver) {
        Optional<PullRequestNotificationTypes> notificationType = byEventClass.entrySet().stream()
                .filter(e -> e.getKey().isAssignableFrom(event.getClass()))
                .map(Map.Entry::getValue)
                .map(type -> {
                    // replace OPENED and MERGED events for auto-merge failure PR with specific types
                    // auto-merge failure PR - is the one that starts with value of bitbucket.automerge.conflicted.pull-request.title resource
                    // default value of it is "Automatic merge failure". this value can be used for testing
                    // it is automatically opened when Bitbucket fails to merge PR changes to underlying branches:
                    // https://confluence.atlassian.com/bitbucketserver052/automatic-branch-merging-935362712.html
                    PullRequestNotificationTypes newType = type;
                    if (startsWith(event.getPullRequest().getTitle(),
                            i18nResolver.getText("bitbucket.automerge.conflicted.pull-request.title"))) {
                        newType = autoMergeTypes.getOrDefault(type, type);
                    }

                    return newType;
                })
                .findAny();
        return notificationType;
    }

}
