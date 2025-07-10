package com.atlassian.confluence.plugins.slack.spi.impl;

import com.atlassian.plugins.slack.api.routes.SlackRoutesProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

import static com.atlassian.confluence.plugins.slack.spi.impl.ConfluenceConfigurationRedirectionManager.FROM_SPACE_ATTRIBUTE_KEY;
import static com.atlassian.confluence.plugins.slack.spi.impl.ConfluenceConfigurationRedirectionManager.SPACE_ATTRIBUTE_KEY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ConfluenceConfigurationRedirectionManagerTest {
    private static final String SPACE_KEY = "SK";
    private static final URI uri = URI.create("/url");

    @Mock
    private ConfluenceSlackRoutesProviderFactory confluenceSlackRoutesProviderFactory;
    @Mock
    private HttpServletRequest httpServletRequest;
    @Mock
    private HttpSession session;
    @Mock
    private SlackRoutesProvider slackRoutesProvider;
    @Captor
    private ArgumentCaptor<Map<String, Object>> mapCaptor;

    @InjectMocks
    private ConfluenceConfigurationRedirectionManager target;

    @Test
    public void getRedirectUri_shouldReturnUri() {
        when(httpServletRequest.getSession()).thenReturn(session);
        when(session.getAttribute(FROM_SPACE_ATTRIBUTE_KEY)).thenReturn(Boolean.TRUE);
        when(session.getAttribute(SPACE_ATTRIBUTE_KEY)).thenReturn(SPACE_KEY);
        when(confluenceSlackRoutesProviderFactory.getProvider(mapCaptor.capture())).thenReturn(slackRoutesProvider);
        when(slackRoutesProvider.getAdminConfigurationPage()).thenReturn(uri);

        Optional<URI> result = target.getRedirectUri(httpServletRequest);

        assertThat(result.isPresent(), is(true));
        assertThat(result.get(), is(uri));
        assertThat(mapCaptor.getValue(), hasEntry("spaceKey", SPACE_KEY));
    }

    @Test
    public void getRedirectUri_shouldReturnEmptyWhenNoRedirect() {
        when(httpServletRequest.getSession()).thenReturn(session);
        when(session.getAttribute(FROM_SPACE_ATTRIBUTE_KEY)).thenReturn(Boolean.FALSE);

        Optional<URI> result = target.getRedirectUri(httpServletRequest);

        assertThat(result.isPresent(), is(false));
    }

    @Test
    public void getRedirectUri_shouldReturnEmptyWhenNoSpaceKey() {
        when(httpServletRequest.getSession()).thenReturn(session);
        when(session.getAttribute(FROM_SPACE_ATTRIBUTE_KEY)).thenReturn(Boolean.TRUE);
        when(session.getAttribute(SPACE_ATTRIBUTE_KEY)).thenReturn(null);

        Optional<URI> result = target.getRedirectUri(httpServletRequest);

        assertThat(result.isPresent(), is(false));
    }
}
