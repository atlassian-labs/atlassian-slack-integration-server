package com.atlassian.bitbucket.plugins.slack.notification;

import com.atlassian.bitbucket.event.branch.BranchCreatedEvent;
import com.atlassian.bitbucket.event.branch.BranchDeletedEvent;
import com.atlassian.bitbucket.event.commit.CommitDiscussionCommentAddedEvent;
import com.atlassian.bitbucket.event.commit.CommitDiscussionCommentDeletedEvent;
import com.atlassian.bitbucket.event.commit.CommitDiscussionCommentEditedEvent;
import com.atlassian.bitbucket.event.commit.CommitDiscussionCommentRepliedEvent;
import com.atlassian.bitbucket.event.repository.RepositoryEvent;
import com.atlassian.bitbucket.event.repository.RepositoryForkedEvent;
import com.atlassian.bitbucket.event.repository.RepositoryPushEvent;
import com.atlassian.bitbucket.event.tag.TagCreatedEvent;
import com.atlassian.bitbucket.event.tag.TagDeletedEvent;
import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.Optional;

public enum RepositoryNotificationTypes {
    PUSHED("CommitPushed"),
    TAG_CREATED("TagCreated"),
    TAG_DELETED("TagDeleted"),
    BRANCH_CREATED("BranchCreated"),
    BRANCH_DELETED("BranchDeleted"),
    FORKED("Forked"),
    COMMENT_ADDED("CommitComment"),
    COMMENT_EDITED("CommitCommentEdited"),
    COMMENT_REPLIED("CommitCommentReplied"),
    COMMENT_DELETED("CommitCommentDeleted");

    private static final Map<Class<? extends RepositoryEvent>, RepositoryNotificationTypes> byEventClass =
            ImmutableMap.<Class<? extends RepositoryEvent>, RepositoryNotificationTypes>builder()
                    .put(RepositoryForkedEvent.class, RepositoryNotificationTypes.FORKED)
                    .put(RepositoryPushEvent.class, RepositoryNotificationTypes.PUSHED)
                    .put(TagCreatedEvent.class, RepositoryNotificationTypes.TAG_CREATED)
                    .put(TagDeletedEvent.class, RepositoryNotificationTypes.TAG_DELETED)
                    .put(BranchCreatedEvent.class, RepositoryNotificationTypes.BRANCH_CREATED)
                    .put(BranchDeletedEvent.class, RepositoryNotificationTypes.BRANCH_DELETED)
                    .put(CommitDiscussionCommentAddedEvent.class, RepositoryNotificationTypes.COMMENT_ADDED)
                    .put(CommitDiscussionCommentEditedEvent.class, RepositoryNotificationTypes.COMMENT_EDITED)
                    .put(CommitDiscussionCommentRepliedEvent.class, RepositoryNotificationTypes.COMMENT_REPLIED)
                    .put(CommitDiscussionCommentDeletedEvent.class, RepositoryNotificationTypes.COMMENT_DELETED)
                    .build();

    private final String key;

    RepositoryNotificationTypes(final String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public static Optional<RepositoryNotificationTypes> byEventClass(final Class<?> clazz) {
        if (RepositoryEvent.class.isAssignableFrom(clazz)) {
            return byEventClass.entrySet().stream()
                    .filter(e -> e.getKey().isAssignableFrom(clazz))
                    .map(Map.Entry::getValue)
                    .findAny();
        }
        return Optional.empty();
    }
}
