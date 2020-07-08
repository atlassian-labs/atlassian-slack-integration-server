package com.atlassian.jira.plugins.slack.bridge.jql.impl;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.index.IssueIndexingService;
import com.atlassian.jira.issue.search.SearchProvider;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.slack.jira.compat.Jira8JqlIndexSearcher;
import com.atlassian.plugin.slack.jira.compat.WithJira8;
import com.atlassian.query.Query;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Optional;
import java.util.concurrent.Callable;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@PrepareForTest({WithJira8.class, Jira8JqlIndexSearcher.class})
@PowerMockIgnore("javax.*")
@RunWith(PowerMockRunner.class)
public class JqlIndexSearcherTest {
    @Mock
    private SearchProvider searchProvider;
    @Mock
    private IssueIndexingService indexingService;

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
        when(searchProvider.searchCount(query, applicationUser)).thenReturn(1L);
        PowerMockito.mockStatic(WithJira8.class);
        when(WithJira8.withJira8(any())).thenReturn(Optional.empty());

        boolean result = target.doesIssueMatchQuery(issue, applicationUser, query);

        assertThat(result, is(true));
        verify(indexingService).reIndex(issue);
    }

    @Test
    public void doesIssueMatchQuery_shouldReturnFalseIfSearchCountIsZero() throws Exception {
        when(searchProvider.searchCount(query, applicationUser)).thenReturn(0L);
        PowerMockito.mockStatic(WithJira8.class);
        when(WithJira8.withJira8(any())).thenReturn(Optional.empty());

        boolean result = target.doesIssueMatchQuery(issue, applicationUser, query);

        assertThat(result, is(false));
        verify(indexingService).reIndex(issue);
    }

    @Test
    public void doesIssueMatchQuery_shouldReturnTrueIfSearchCountOverrideSecurityGreaterThanZero() throws Exception {
        when(searchProvider.searchCountOverrideSecurity(query, null)).thenReturn(1L);
        PowerMockito.mockStatic(WithJira8.class);
        when(WithJira8.withJira8(any())).thenReturn(Optional.empty());

        boolean result = target.doesIssueMatchQuery(issue, null, query);

        assertThat(result, is(true));
        verify(indexingService).reIndex(issue);
    }

    @Test
    public void doesIssueMatchQuery_shouldReturnFalseIfSearchOverrideSecurityCountIsZero() throws Exception {
        when(searchProvider.searchCountOverrideSecurity(query, null)).thenReturn(0L);
        PowerMockito.mockStatic(WithJira8.class);
        when(WithJira8.withJira8(any())).thenReturn(Optional.empty());

        boolean result = target.doesIssueMatchQuery(issue, null, query);

        assertThat(result, is(false));
        verify(indexingService).reIndex(issue);
    }

    @Test
    public void doesIssueMatchQuery_shouldReturnTrueWithJira8GreaterThanZero() throws Exception {
        PowerMockito.mockStatic(WithJira8.class);
        when(WithJira8.withJira8(any())).thenReturn(Optional.of(1L));

        boolean result = target.doesIssueMatchQuery(issue, applicationUser, query);

        assertThat(result, is(true));
        verify(indexingService).reIndex(issue);
        verify(indexingService).reIndex(issue);
    }

    @Test
    public void doesIssueMatchQuery_shouldReturnFalseWithJira8CountIsZero() throws Exception {
        PowerMockito.mockStatic(WithJira8.class);
        when(WithJira8.withJira8(any())).thenReturn(Optional.of(0L));

        boolean result = target.doesIssueMatchQuery(issue, applicationUser, query);

        assertThat(result, is(false));
        verify(indexingService).reIndex(issue);
    }

    @Test
    public void doesIssueMatchQuery_shouldDelegateProperlyToJira8() throws Exception {
        PowerMockito.mockStatic(WithJira8.class);
        PowerMockito.mockStatic(Jira8JqlIndexSearcher.class);
        when(WithJira8.withJira8(captor.capture())).thenReturn(Optional.of(1L));
        when(Jira8JqlIndexSearcher.searchCount(searchProvider, applicationUser, query)).thenReturn(1L);

        //call but does not use if here
        target.doesIssueMatchQuery(issue, applicationUser, query);

        long result = captor.getValue().call();

        assertThat(result, is(1L));
        verify(indexingService).reIndex(issue);
    }
}
