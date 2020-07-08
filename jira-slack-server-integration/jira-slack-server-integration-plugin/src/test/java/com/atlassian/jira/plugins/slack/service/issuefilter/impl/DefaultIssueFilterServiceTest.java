package com.atlassian.jira.plugins.slack.service.issuefilter.impl;

import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugins.slack.model.EventFilterType;
import com.atlassian.jira.plugins.slack.model.ProjectConfiguration;
import com.atlassian.jira.plugins.slack.service.issuefilter.IssueFilter;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class DefaultIssueFilterServiceTest {
    @Mock
    private ProjectConfiguration projectConfiguration1;
    @Mock
    private ProjectConfiguration projectConfiguration2;
    @Mock
    private IssueFilter issueFilter1;
    @Mock
    private IssueFilter issueFilter2;
    @Mock
    private Issue issue;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Test
    public void apply_shouldReturnTrueIfAllFilterMatch() {
        List<IssueFilter> filters = Arrays.asList(issueFilter1, issueFilter2);
        when(issueFilter1.getEventFilterType()).thenReturn(EventFilterType.ISSUE_TYPE);
        when(issueFilter2.getEventFilterType()).thenReturn(EventFilterType.JQL_QUERY);

        List<ProjectConfiguration> configs = Arrays.asList(projectConfiguration1, projectConfiguration2);
        when(projectConfiguration1.getName()).thenReturn(EventFilterType.ISSUE_TYPE.getDbKey());
        when(projectConfiguration1.getValue()).thenReturn("V1");
        when(projectConfiguration2.getName()).thenReturn(EventFilterType.JQL_QUERY.getDbKey());
        when(projectConfiguration2.getValue()).thenReturn("V2");

        IssueEvent issueEvent = new IssueEvent(issue, Collections.emptyMap(), null, 0L);

        when(issueFilter1.apply(issueEvent, "V1")).thenReturn(true);
        when(issueFilter2.apply(issueEvent, "V2")).thenReturn(true);

        DefaultIssueFilterService target = new DefaultIssueFilterService(filters);
        boolean result = target.apply(issueEvent, configs);

        assertThat(result, is(true));
    }

    @Test
    public void apply_shouldReturnFalseIfOneFilterDoesNotMatch() {
        List<IssueFilter> filters = Arrays.asList(issueFilter1, issueFilter2);
        when(issueFilter1.getEventFilterType()).thenReturn(EventFilterType.ISSUE_TYPE);
        when(issueFilter2.getEventFilterType()).thenReturn(EventFilterType.JQL_QUERY);

        List<ProjectConfiguration> configs = Arrays.asList(projectConfiguration1, projectConfiguration2);
        when(projectConfiguration1.getName()).thenReturn(EventFilterType.ISSUE_TYPE.getDbKey());
        when(projectConfiguration1.getValue()).thenReturn("V1");
        when(projectConfiguration2.getName()).thenReturn(EventFilterType.JQL_QUERY.getDbKey());
        when(projectConfiguration2.getValue()).thenReturn("V2");

        IssueEvent issueEvent = new IssueEvent(issue, Collections.emptyMap(), null, 0L);

        when(issueFilter1.apply(issueEvent, "V1")).thenReturn(true);
        when(issueFilter2.apply(issueEvent, "V2")).thenReturn(false);

        DefaultIssueFilterService target = new DefaultIssueFilterService(filters);
        boolean result = target.apply(issueEvent, configs);

        assertThat(result, is(false));
    }
}
