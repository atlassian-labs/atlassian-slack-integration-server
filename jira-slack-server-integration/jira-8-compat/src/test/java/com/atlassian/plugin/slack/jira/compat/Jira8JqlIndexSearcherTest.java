package com.atlassian.plugin.slack.jira.compat;

import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchProvider;
import com.atlassian.jira.issue.search.SearchQuery;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.query.Query;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class Jira8JqlIndexSearcherTest {
    @Mock
    private SearchProvider searchProvider;
    @Mock
    private ApplicationUser caller;
    @Mock
    private Query query;

    @Captor
    private ArgumentCaptor<SearchQuery> searchQueryArgumentCaptor;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Test
    public void searchCount_shouldReturnExpectedValue() throws SearchException {
        when(searchProvider.getHitCount(searchQueryArgumentCaptor.capture())).thenReturn(7L);

        long result = Jira8JqlIndexSearcher.searchCount(searchProvider, caller, query);

        assertThat(result, Matchers.is(7L));

        assertThat(searchQueryArgumentCaptor.getValue().getQuery(), sameInstance(query));
        assertThat(searchQueryArgumentCaptor.getValue().getUser(), sameInstance(caller));
        assertThat(searchQueryArgumentCaptor.getValue().isOverrideSecurity(), is(false));
    }

    @Test
    public void searchCount_shouldReturnExpectedValueWhenUserIsNull() throws SearchException {
        when(searchProvider.getHitCount(searchQueryArgumentCaptor.capture())).thenReturn(7L);
        when(searchProvider.getHitCount(searchQueryArgumentCaptor.capture())).thenReturn(7L);

        long result = Jira8JqlIndexSearcher.searchCount(searchProvider, null, query);

        assertThat(result, Matchers.is(7L));

        assertThat(searchQueryArgumentCaptor.getValue().getQuery(), sameInstance(query));
        assertThat(searchQueryArgumentCaptor.getValue().getUser(), nullValue());
        assertThat(searchQueryArgumentCaptor.getValue().isOverrideSecurity(), is(true));
    }
}
