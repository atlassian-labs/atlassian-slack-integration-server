package com.atlassian.jira.plugins.slack.service.issuefilter.impl;

import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.issue.Issue;
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
import static org.mockito.Mockito.verify;
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
        when(issue.getIssueTypeId()).thenReturn("B");

        assertThat(target.apply(issueEvent, "ALL"), is(true));
        assertThat(target.apply(issueEvent, ""), is(true));
        assertThat(target.apply(issueEvent, null), is(true));
        assertThat(target.apply(issueEvent, "B"), is(true));
        assertThat(target.apply(issueEvent, "B,T"), is(true));
        assertThat(target.apply(issueEvent, "T"), is(false));
    }

    @Test
    public void apply_shouldReturnFalseWhenIssueTypeIdIsEmpty() {
        IssueEvent issueEvent = new IssueEvent(issue, Collections.emptyMap(), null, 0L);
        when(issue.getIssueTypeId()).thenReturn("");

        assertThat(target.apply(issueEvent, "ALL"), is(true));
        assertThat(target.apply(issueEvent, ""), is(true));
        assertThat(target.apply(issueEvent, null), is(true));
        assertThat(target.apply(issueEvent, "B"), is(false));
        assertThat(target.apply(issueEvent, "B,T"), is(false));
        assertThat(target.apply(issueEvent, "T"), is(false));
    }

    @Test
    public void apply_shouldReturnFalseWhenIssueTypeIdIsNull() {
        IssueEvent issueEvent = new IssueEvent(issue, Collections.emptyMap(), null, 0L);
        when(issue.getIssueTypeId()).thenReturn(null);

        assertThat(target.apply(issueEvent, "ALL"), is(true));
        assertThat(target.apply(issueEvent, ""), is(true));
        assertThat(target.apply(issueEvent, null), is(true));
        assertThat(target.apply(issueEvent, "B"), is(false));
        assertThat(target.apply(issueEvent, "B,T"), is(false));
        assertThat(target.apply(issueEvent, "T"), is(false));
    }

    @Test
    public void getEventFilterType() {
        assertThat(target.getEventFilterType(), is(EventFilterType.ISSUE_TYPE));
    }
}
