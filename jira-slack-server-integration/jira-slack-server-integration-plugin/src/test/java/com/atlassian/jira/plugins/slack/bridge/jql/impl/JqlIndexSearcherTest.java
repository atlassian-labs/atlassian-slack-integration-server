package com.atlassian.jira.plugins.slack.bridge.jql.impl;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.search.issue.IssueDocumentSearchService;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.query.Query;
import java.util.concurrent.Callable;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class JqlIndexSearcherTest {
    @Mock
    private IssueDocumentSearchService issueDocumentSearchService;

    @Mock
    private ApplicationUser applicationUser;
    @Mock
    private Query query;
    @Mock
    private Issue issue;

    @Captor
    private ArgumentCaptor<Callable<Long>> captor;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private JqlIndexSearcher target;

    @Test
    public void doesIssueMatchQuery_shouldReturnTrueIfSearchCountGreaterThanZero() throws Exception {
        when(issueDocumentSearchService.getHitCount(applicationUser, query, false)).thenReturn(1L);

        boolean result = target.doesIssueMatchQuery(issue, applicationUser, query);

        assertThat(result, is(true));
    }

    @Test
    public void doesIssueMatchQuery_shouldReturnFalseIfSearchCountIsZero() throws Exception {
        when(issueDocumentSearchService.getHitCount(applicationUser, query, false)).thenReturn(0L);

        boolean result = target.doesIssueMatchQuery(issue, applicationUser, query);

        assertThat(result, is(false));
    }

    @Test
    public void doesIssueMatchQuery_shouldReturnTrueIfSearchCountOverrideSecurityGreaterThanZero() throws Exception {
        when(issueDocumentSearchService.getHitCount(null, query, true)).thenReturn(1L);

        boolean result = target.doesIssueMatchQuery(issue, null, query);

        assertThat(result, is(true));
    }

    @Test
    public void doesIssueMatchQuery_shouldReturnFalseIfSearchOverrideSecurityCountIsZero() throws Exception {
        when(issueDocumentSearchService.getHitCount(applicationUser, query, false)).thenReturn(0L);

        boolean result = target.doesIssueMatchQuery(issue, null, query);

        assertThat(result, is(false));
    }
}
