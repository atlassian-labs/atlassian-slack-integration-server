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

    public static String removeJiraTags(final @Nullable String commentBody) {
        if (commentBody == null) {
            return "";
        }
        return commentBody
                .replaceAll("\\{color(?::[^}]+)?}", "")
                .replace("{quote}", "")
                .replace("{noformat}", "")
                .replaceAll("\\{panel(?::[^}]+)?}", "")
                .replaceAll("\\{code(?::[^}]+)?}", "");
    }
}
