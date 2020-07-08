package com.atlassian.jira.plugins.slack.util;

import com.atlassian.jira.issue.comments.Comment;
import lombok.experimental.UtilityClass;

import javax.annotation.Nullable;

@UtilityClass
public class CommentUtil {
    public static boolean isRestricted(final @Nullable Comment comment) {
        return comment != null
                && (comment.getGroupLevel() != null || comment.getRoleLevelId() != null);
    }
}
