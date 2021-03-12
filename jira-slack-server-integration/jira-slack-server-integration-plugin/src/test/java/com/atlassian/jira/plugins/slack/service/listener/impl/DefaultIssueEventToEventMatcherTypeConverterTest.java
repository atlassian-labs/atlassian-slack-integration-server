package com.atlassian.jira.plugins.slack.service.listener.impl;

import com.atlassian.jira.event.issue.IssueChangedEvent;
import com.atlassian.jira.event.issue.IssueChangedEventImpl;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.event.type.EventType;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.comments.Comment;
import com.atlassian.jira.issue.history.ChangeItemBean;
import com.atlassian.jira.plugins.slack.model.EventMatcherType;
import com.atlassian.jira.plugins.slack.util.changelog.ChangeLogExtractor;
import com.atlassian.jira.plugins.slack.util.changelog.ChangeLogItem;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class DefaultIssueEventToEventMatcherTypeConverterTest {
    @Mock
    private ChangeLogExtractor changeLogExtractor;

    @Mock
    private ChangeLogItem changeLogItem;
    @Mock
    private Issue issue;
    @Mock
    private Comment comment;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private DefaultIssueEventToEventMatcherTypeConverter target;

    @Test
    public void match_forIssueAssignment() {
        IssueEvent issueEvent = new IssueEvent(issue, Collections.emptyMap(), null, EventType.ISSUE_ASSIGNED_ID);
        when(changeLogExtractor.getChanges(issueEvent)).thenReturn(Collections.singletonList(changeLogItem));
        when(changeLogItem.getField()).thenReturn(ChangeLogExtractor.ASSIGNEE_FIELD_NAME);

        Collection<EventMatcherType> result = target.match(issueEvent);

        assertThat(result, contains(EventMatcherType.ISSUE_ASSIGNMENT_CHANGED));
    }

    @Test
    public void match_forIssueTransition() {
        List<ChangeItemBean> changeItems = Collections.singletonList(new ChangeItemBean("fieldType",
                ChangeLogExtractor.STATUS_FIELD_NAME, null, null));
        IssueChangedEvent event = new IssueChangedEventImpl(issue, Optional.empty(), changeItems, Optional.empty(), new Date(), false, null);

        Collection<EventMatcherType> result = target.match(event);

        assertThat(result, contains(EventMatcherType.ISSUE_TRANSITIONED));
    }

    @Test
    public void match_forIssueCommented() {
        IssueEvent issueEvent = new IssueEvent(issue, null, comment, null, null, Collections.emptyMap(), EventType.ISSUE_COMMENTED_ID);
        when(changeLogExtractor.getChanges(issueEvent)).thenReturn(Collections.singletonList(changeLogItem));
        when(changeLogItem.getField()).thenReturn("");

        Collection<EventMatcherType> result = target.match(issueEvent);

        assertThat(result, contains(EventMatcherType.ISSUE_COMMENTED));
    }
}
