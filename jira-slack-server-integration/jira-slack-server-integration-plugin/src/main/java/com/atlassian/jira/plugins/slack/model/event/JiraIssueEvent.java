package com.atlassian.jira.plugins.slack.model.event;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.comments.Comment;
import com.atlassian.jira.plugins.slack.model.EventMatcherType;
import com.atlassian.jira.plugins.slack.util.changelog.ChangeLogItem;
import com.atlassian.jira.user.ApplicationUser;

import java.util.List;
import java.util.Optional;

/**
 * This interface contains information about an issue event sent from Jira.
 */
public interface JiraIssueEvent extends PluginEvent {
    EventMatcherType getEventMatcher();
    Optional<ApplicationUser> getEventAuthor();
    Issue getIssue();
    Optional<Comment> getComment();
    List<ChangeLogItem> getChangeLog();
}
