package com.atlassian.bitbucket.plugins.slack.notification;

import com.atlassian.bitbucket.comment.CommentAction;
import com.atlassian.bitbucket.comment.CommentState;
import com.atlassian.bitbucket.event.pull.PullRequestCommentEditedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestCommentEvent;

import static com.atlassian.bitbucket.comment.CommentSeverity.BLOCKER;
import static com.google.common.base.Preconditions.checkArgument;

public enum TaskNotificationTypes {
    CREATED("TaskCreated"),
    DELETED("TaskDeleted"),
    UPDATED("TaskUpdated"),
    RESOLVED("TaskResolved"),
    REOPENED("TaskReopened");

    private final String key;

    TaskNotificationTypes(final String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public static TaskNotificationTypes from(final PullRequestCommentEvent event) {
        checkArgument(event.getComment().getSeverity() == BLOCKER,
                "event is not for BLOCKER comment");
        CommentAction commentAction = event.getCommentAction();
        switch (commentAction) {
            case ADDED:
            case REPLIED:
                return CREATED;
            case DELETED:
                return DELETED;
            case EDITED:
                PullRequestCommentEditedEvent editedEvent = (PullRequestCommentEditedEvent) event;
                if (editedEvent.getPreviousState() == CommentState.RESOLVED &&
                        event.getComment().getState() == CommentState.OPEN) {
                    return TaskNotificationTypes.REOPENED;
                }
                if (editedEvent.getPreviousState() == CommentState.OPEN &&
                        event.getComment().getState() == CommentState.RESOLVED) {
                    return TaskNotificationTypes.RESOLVED;
                }
                return UPDATED;
            default:
                throw new IllegalArgumentException("Unknown comment action: " + commentAction);
        }
    }
}
