package com.atlassian.plugins.slack.admin;

import com.atlassian.sal.api.auth.LoginUriProvider;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LoginRedirectionManagerTest {
    @Mock
    private LoginUriProvider loginUriProvider;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private HttpSession session;
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private LoginRedirectionManager loginRedirectionManager;

    @Test
    public void testLoginRedirectionRemovesContext() throws IOException {
        final String loginUri = "http://www.example.com/context/abc";
        when(loginUriProvider.getLoginUri(any(URI.class))).thenReturn(URI.create(loginUri));
        when(request.getRequestURI()).thenReturn("http://www.atlassian.com/context/abc?abcd=123");
        when(request.getQueryString()).thenReturn("abcd=123");
        when(request.getContextPath()).thenReturn("/context");
        when(request.getSession()).thenReturn(session);

        //when:
        loginRedirectionManager.redirectToLogin(request, response);

        final ArgumentCaptor<String> url = ArgumentCaptor.forClass(String.class);
        verify(response).sendRedirect(url.capture());
        assertEquals("Returned url should be the same as the value returned from the uri provider", loginUri, url.getValue());

        final ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(loginUriProvider).getLoginUri(uriCaptor.capture());
        final URI uriValue = uriCaptor.getValue();
        assertEquals("The URI passed to the login redirection manager should have the correct path", "/abc", uriValue.getPath());
        assertEquals("The URI passed to the login redirection manager should have the correct query params", "abcd=123", uriValue.getQuery());
    }
}
