package com.atlassian.jira.plugins.slack.service.issuefilter.impl;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.index.IssueIndexingParams;
import com.atlassian.jira.issue.index.IssueIndexingService;
import com.atlassian.jira.jql.builder.ConditionBuilder;
import com.atlassian.jira.jql.builder.JqlClauseBuilder;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.plugins.slack.bridge.jql.JqlSearcher;
import com.atlassian.jira.plugins.slack.model.EventFilterType;
import com.atlassian.jira.plugins.slack.model.EventMatcherType;
import com.atlassian.jira.plugins.slack.model.event.DefaultJiraIssueEvent;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.MessageSet;
import com.atlassian.query.Query;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JqlIssueFilterTest {
    @Mock
    private SearchService searchService;
    @Mock
    private JqlSearcher searcher;
    @Mock
    private IssueIndexingService indexingService;

    @Mock
    private ApplicationUser applicationUser;
    @Mock
    private Issue issue;
    @Mock
    private Query query;
    @Mock
    private Query newQuery;
    @Mock
    private MessageSet messageSet;
    @Mock
    private JqlQueryBuilder jqlQueryBuilder;
    @Mock
    private JqlClauseBuilder jqlClauseBuilder;
    @Mock
    private ConditionBuilder conditionBuilder;
    @Mock
    private JqlIssueFilter.Sleeper mockSleeper;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private MockedStatic<JqlQueryBuilder> mockedJqlQueryBuilder;

    @InjectMocks
    private JqlIssueFilter target;

    @Before
    public void setUp() {
        target = new JqlIssueFilter(searchService, searcher, indexingService, mockSleeper);
    }

    @After
    public void tearDown() {
        if (mockedJqlQueryBuilder != null) {
            mockedJqlQueryBuilder.close();
        }
    }

    @Test
    public void apply_shouldReturnTrueWhenJqlIsEmpty() {
        IssueEvent issueEvent = new IssueEvent(issue, Collections.emptyMap(), null, 0L);
        DefaultJiraIssueEvent jiraIssueEvent = DefaultJiraIssueEvent.of(EventMatcherType.ISSUE_CREATED, issueEvent, emptyList());

        assertThat(target.apply(jiraIssueEvent, ""), is(true));
        assertThat(target.apply(jiraIssueEvent, null), is(true));
    }

    @Test
    public void getEventFilterType() {
        assertThat(target.getEventFilterType(), is(EventFilterType.JQL_QUERY));
    }

    @Test
    public void matchesJql() throws Exception {
        SearchService.ParseResult parseResult = new SearchService.ParseResult(query, messageSet);
        when(messageSet.hasAnyErrors()).thenReturn(false);
        when(issue.getKey()).thenReturn("TEST-123");
        when(searchService.parseQuery(applicationUser, "Q")).thenReturn(parseResult);

        mockedJqlQueryBuilder = mockStatic(JqlQueryBuilder.class);
        mockedJqlQueryBuilder.when(() -> JqlQueryBuilder.newBuilder(query)).thenReturn(jqlQueryBuilder);
        mockedJqlQueryBuilder.when(() -> JqlQueryBuilder.newBuilder()).thenReturn(jqlQueryBuilder);
        when(jqlQueryBuilder.where()).thenReturn(jqlClauseBuilder);
        when(jqlClauseBuilder.and()).thenReturn(jqlClauseBuilder);
        when(jqlClauseBuilder.issue()).thenReturn(conditionBuilder);
        when(conditionBuilder.eq("TEST-123")).thenReturn(jqlClauseBuilder);
        when(jqlClauseBuilder.buildQuery()).thenReturn(newQuery);
        when(searcher.doesIssueMatchQuery(issue, null, newQuery)).thenReturn(true);

        boolean matchingResult = target.matchesJql("Q", issue, Optional.of(applicationUser));

        assertThat(matchingResult, is(true));
        verify(indexingService).reIndex(issue, IssueIndexingParams.INDEX_ISSUE_ONLY);
    }

}
