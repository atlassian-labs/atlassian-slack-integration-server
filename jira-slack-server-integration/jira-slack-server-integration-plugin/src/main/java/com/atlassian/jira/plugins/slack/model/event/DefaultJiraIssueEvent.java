package com.atlassian.jira.plugins.slack.model.event;

import com.atlassian.jira.event.issue.IssueChangedEvent;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.comments.Comment;
import com.atlassian.jira.issue.history.ChangeItemBean;
import com.atlassian.jira.plugins.slack.model.EventMatcherType;
import com.atlassian.jira.plugins.slack.util.changelog.ChangeLogItem;
import com.atlassian.jira.user.ApplicationUser;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Getter
public class DefaultJiraIssueEvent implements JiraIssueEvent {
    private final EventMatcherType eventMatcher;
    private final Optional<ApplicationUser> eventAuthor;
    private final Issue issue;
    private final Optional<Comment> comment;
    private final List<ChangeLogItem> changeLog;

    public static DefaultJiraIssueEvent of(final EventMatcherType eventMatcher,
                                           final IssueEvent issueEvent,
                                           final List<ChangeLogItem> changeLog) {
        return new DefaultJiraIssueEvent(eventMatcher, Optional.ofNullable(issueEvent.getUser()), issueEvent.getIssue(),
                Optional.ofNullable(issueEvent.getComment()), changeLog);
    }

    public static DefaultJiraIssueEvent of(final EventMatcherType eventMatcher,
                                           final IssueChangedEvent issueChangedEvent) {
        Collection<ChangeItemBean> changeItems = issueChangedEvent.getChangeItems();
        List<ChangeLogItem> changeLog = changeItems.stream()
                .map(changeItem -> new ChangeLogItem(changeItem.getField(), changeItem.getToString(), changeItem.getTo(),
                        changeItem.getFromString(), changeItem.getFrom()))
                .collect(Collectors.toCollection(() -> new ArrayList<>(changeItems.size())));

        return new DefaultJiraIssueEvent(eventMatcher, issueChangedEvent.getAuthor(), issueChangedEvent.getIssue(),
                issueChangedEvent.getComment(), changeLog);
    }
}
