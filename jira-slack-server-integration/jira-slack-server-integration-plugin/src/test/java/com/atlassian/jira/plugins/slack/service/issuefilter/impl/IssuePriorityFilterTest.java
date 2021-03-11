package com.atlassian.jira.plugins.slack.service.issuefilter.impl;

import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.priority.Priority;
import com.atlassian.jira.plugins.slack.model.EventFilterType;
import com.atlassian.jira.plugins.slack.model.EventMatcherType;
import com.atlassian.jira.plugins.slack.model.event.DefaultJiraIssueEvent;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;

import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class IssuePriorityFilterTest {
    @Mock
    private Issue issue;
    @Mock
    private Priority priority;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private IssuePriorityFilter target;

    @Test
    public void apply() {
        IssueEvent issueEvent = new IssueEvent(issue, Collections.emptyMap(), null, 0L);
        DefaultJiraIssueEvent jiraIssueEvent = DefaultJiraIssueEvent.of(EventMatcherType.ISSUE_CREATED, issueEvent, emptyList());
        when(issue.getPriority()).thenReturn(priority);
        when(priority.getId()).thenReturn("M");

        assertThat(target.apply(jiraIssueEvent, "ALL"), is(true));
        assertThat(target.apply(jiraIssueEvent, ""), is(true));
        assertThat(target.apply(jiraIssueEvent, null), is(true));
        assertThat(target.apply(jiraIssueEvent, "M"), is(true));
        assertThat(target.apply(jiraIssueEvent, "H,M"), is(true));
        assertThat(target.apply(jiraIssueEvent, "H"), is(false));
    }

    @Test
    public void apply_shouldReturnFalseWhenValueIsEmpty() {
        IssueEvent issueEvent = new IssueEvent(issue, Collections.emptyMap(), null, 0L);
        DefaultJiraIssueEvent jiraIssueEvent = DefaultJiraIssueEvent.of(EventMatcherType.ISSUE_CREATED, issueEvent, emptyList());
        when(issue.getPriority()).thenReturn(priority);
        when(priority.getId()).thenReturn("");

        assertThat(target.apply(jiraIssueEvent, "ALL"), is(true));
        assertThat(target.apply(jiraIssueEvent, ""), is(true));
        assertThat(target.apply(jiraIssueEvent, null), is(true));
        assertThat(target.apply(jiraIssueEvent, "M"), is(false));
        assertThat(target.apply(jiraIssueEvent, "H,M"), is(false));
        assertThat(target.apply(jiraIssueEvent, "H"), is(false));
    }

    @Test
    public void apply_shouldReturnFalseWhenPriorityIsNull() {
        IssueEvent issueEvent = new IssueEvent(issue, Collections.emptyMap(), null, 0L);
        DefaultJiraIssueEvent jiraIssueEvent = DefaultJiraIssueEvent.of(EventMatcherType.ISSUE_CREATED, issueEvent, emptyList());
        when(issue.getPriority()).thenReturn(null);

        assertThat(target.apply(jiraIssueEvent, "ALL"), is(true));
        assertThat(target.apply(jiraIssueEvent, ""), is(true));
        assertThat(target.apply(jiraIssueEvent, null), is(true));
        assertThat(target.apply(jiraIssueEvent, "M"), is(false));
        assertThat(target.apply(jiraIssueEvent, "H,M"), is(false));
        assertThat(target.apply(jiraIssueEvent, "H"), is(false));
    }

    @Test
    public void getEventFilterType() {
        assertThat(target.getEventFilterType(), is(EventFilterType.ISSUE_PRIORITY));
    }
}
