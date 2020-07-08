package com.atlassian.jira.plugins.slack.service.issuefilter.impl;

import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.priority.Priority;
import com.atlassian.jira.plugins.slack.model.EventFilterType;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;

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
        when(issue.getPriority()).thenReturn(priority);
        when(priority.getId()).thenReturn("M");

        assertThat(target.apply(issueEvent, "ALL"), is(true));
        assertThat(target.apply(issueEvent, ""), is(true));
        assertThat(target.apply(issueEvent, null), is(true));
        assertThat(target.apply(issueEvent, "M"), is(true));
        assertThat(target.apply(issueEvent, "H,M"), is(true));
        assertThat(target.apply(issueEvent, "H"), is(false));
    }

    @Test
    public void apply_shouldReturnFalseWhenValueIsEmpty() {
        IssueEvent issueEvent = new IssueEvent(issue, Collections.emptyMap(), null, 0L);
        when(issue.getPriority()).thenReturn(priority);
        when(priority.getId()).thenReturn("");

        assertThat(target.apply(issueEvent, "ALL"), is(true));
        assertThat(target.apply(issueEvent, ""), is(true));
        assertThat(target.apply(issueEvent, null), is(true));
        assertThat(target.apply(issueEvent, "M"), is(false));
        assertThat(target.apply(issueEvent, "H,M"), is(false));
        assertThat(target.apply(issueEvent, "H"), is(false));
    }

    @Test
    public void apply_shouldReturnFalseWhenPriorityIsNull() {
        IssueEvent issueEvent = new IssueEvent(issue, Collections.emptyMap(), null, 0L);
        when(issue.getPriority()).thenReturn(null);

        assertThat(target.apply(issueEvent, "ALL"), is(true));
        assertThat(target.apply(issueEvent, ""), is(true));
        assertThat(target.apply(issueEvent, null), is(true));
        assertThat(target.apply(issueEvent, "M"), is(false));
        assertThat(target.apply(issueEvent, "H,M"), is(false));
        assertThat(target.apply(issueEvent, "H"), is(false));
    }

    @Test
    public void getEventFilterType() {
        assertThat(target.getEventFilterType(), is(EventFilterType.ISSUE_PRIORITY));
    }
}
