package com.atlassian.jira.plugins.slack.spi.impl;

import com.atlassian.plugins.slack.api.routes.SlackRoutesProvider;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.net.URI;
import java.util.Map;
import java.util.Optional;

import static com.atlassian.jira.plugins.slack.spi.impl.JiraConfigurationRedirectionManager.FROM_PROJECT_ATTRIBUTE_KEY;
import static com.atlassian.jira.plugins.slack.spi.impl.JiraConfigurationRedirectionManager.PROJECT_ATTRIBUTE_KEY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

public class JiraConfigurationRedirectionManagerTest {
    private static final String PROJECT_KEY = "PK";
    private static final URI uri = URI.create("/url");

    @Mock
    private JiraSlackRoutesProviderFactory jiraSlackRoutesProviderFactory;
    @Mock
    private HttpServletRequest httpServletRequest;
    @Mock
    private HttpSession session;
    @Mock
    private SlackRoutesProvider slackRoutesProvider;
    @Captor
    private ArgumentCaptor<Map<String, Object>> mapCaptor;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private JiraConfigurationRedirectionManager target;

    @Test
    public void getRedirectUri_shouldReturnUri() {
        when(httpServletRequest.getSession(false)).thenReturn(session);
        when(session.getAttribute(FROM_PROJECT_ATTRIBUTE_KEY)).thenReturn(Boolean.TRUE);
        when(session.getAttribute(PROJECT_ATTRIBUTE_KEY)).thenReturn(PROJECT_KEY);
        when(jiraSlackRoutesProviderFactory.getProvider(mapCaptor.capture())).thenReturn(slackRoutesProvider);
        when(slackRoutesProvider.getAdminConfigurationPage()).thenReturn(uri);

        Optional<URI> result = target.getRedirectUri(httpServletRequest);

        assertThat(result.isPresent(), is(true));
        assertThat(result.get(), is(uri));
        assertThat(mapCaptor.getValue(), hasEntry("projectKey", PROJECT_KEY));
    }

    @Test
    public void getRedirectUri_shouldReturnEmptyWhenNoRedirect() {
        when(httpServletRequest.getSession(false)).thenReturn(session);
        when(session.getAttribute(FROM_PROJECT_ATTRIBUTE_KEY)).thenReturn(Boolean.FALSE);

        Optional<URI> result = target.getRedirectUri(httpServletRequest);

        assertThat(result.isPresent(), is(false));
    }

    @Test
    public void getRedirectUri_shouldReturnEmptyWhenNoProjectKey() {
        when(httpServletRequest.getSession(false)).thenReturn(session);
        when(session.getAttribute(FROM_PROJECT_ATTRIBUTE_KEY)).thenReturn(Boolean.TRUE);
        when(session.getAttribute(PROJECT_ATTRIBUTE_KEY)).thenReturn(null);

        Optional<URI> result = target.getRedirectUri(httpServletRequest);

        assertThat(result.isPresent(), is(false));
    }
}
