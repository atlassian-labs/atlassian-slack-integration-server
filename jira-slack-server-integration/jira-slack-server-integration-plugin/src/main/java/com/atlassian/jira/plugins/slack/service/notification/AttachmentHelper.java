package com.atlassian.jira.plugins.slack.service.notification;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.comments.Comment;
import com.atlassian.jira.user.ApplicationUser;
import com.github.seratch.jslack.api.model.Attachment;
import com.github.seratch.jslack.api.model.Field;

import java.util.List;

public interface AttachmentHelper {
    Attachment buildIssueAttachment(String pretext, Issue issue, List<Field> fields);

    Attachment buildCommentAttachment(String pretext, Issue issue, Comment comment);

    String jiraTitle();

    String jiraUrl();

    String issueUrl(String issueKey);

    String issueLink(Issue issue);

    String userLink(ApplicationUser user);

    String projectUrl(String projectKey);

    String getAccountMessage(ApplicationUser user);
}
