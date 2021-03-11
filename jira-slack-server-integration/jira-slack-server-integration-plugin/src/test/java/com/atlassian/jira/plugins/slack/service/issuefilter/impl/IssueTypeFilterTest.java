package com.atlassian.jira.plugins.slack.service.issuefilter.impl;

import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.issue.Issue;
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

public class IssueTypeFilterTest {
    @Mock
    private Issue issue;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private IssueTypeFilter target;

    @Test
    public void apply_shouldReturnTrueWhenTypeMatches() {
        IssueEvent issueEvent = new IssueEvent(issue, Collections.emptyMap(), null, 0L);
        DefaultJiraIssueEvent jiraIssueEvent = DefaultJiraIssueEvent.of(EventMatcherType.ISSUE_CREATED, issueEvent, emptyList());
        when(issue.getIssueTypeId()).thenReturn("B");

        assertThat(target.apply(jiraIssueEvent, "ALL"), is(true));
        assertThat(target.apply(jiraIssueEvent, ""), is(true));
        assertThat(target.apply(jiraIssueEvent, null), is(true));
        assertThat(target.apply(jiraIssueEvent, "B"), is(true));
        assertThat(target.apply(jiraIssueEvent, "B,T"), is(true));
        assertThat(target.apply(jiraIssueEvent, "T"), is(false));
    }

    @Test
    public void apply_shouldReturnFalseWhenIssueTypeIdIsEmpty() {
        IssueEvent issueEvent = new IssueEvent(issue, Collections.emptyMap(), null, 0L);
        DefaultJiraIssueEvent jiraIssueEvent = DefaultJiraIssueEvent.of(EventMatcherType.ISSUE_CREATED, issueEvent, emptyList());
        when(issue.getIssueTypeId()).thenReturn("");

        assertThat(target.apply(jiraIssueEvent, "ALL"), is(true));
        assertThat(target.apply(jiraIssueEvent, ""), is(true));
        assertThat(target.apply(jiraIssueEvent, null), is(true));
        assertThat(target.apply(jiraIssueEvent, "B"), is(false));
        assertThat(target.apply(jiraIssueEvent, "B,T"), is(false));
        assertThat(target.apply(jiraIssueEvent, "T"), is(false));
    }

    @Test
    public void apply_shouldReturnFalseWhenIssueTypeIdIsNull() {
        IssueEvent issueEvent = new IssueEvent(issue, Collections.emptyMap(), null, 0L);
        DefaultJiraIssueEvent jiraIssueEvent = DefaultJiraIssueEvent.of(EventMatcherType.ISSUE_CREATED, issueEvent, emptyList());
        when(issue.getIssueTypeId()).thenReturn(null);

        assertThat(target.apply(jiraIssueEvent, "ALL"), is(true));
        assertThat(target.apply(jiraIssueEvent, ""), is(true));
        assertThat(target.apply(jiraIssueEvent, null), is(true));
        assertThat(target.apply(jiraIssueEvent, "B"), is(false));
        assertThat(target.apply(jiraIssueEvent, "B,T"), is(false));
        assertThat(target.apply(jiraIssueEvent, "T"), is(false));
    }

    @Test
    public void getEventFilterType() {
        assertThat(target.getEventFilterType(), is(EventFilterType.ISSUE_TYPE));
    }
}
