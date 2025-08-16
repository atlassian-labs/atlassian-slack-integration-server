package com.atlassian.plugins.slack.admin;

import com.atlassian.plugins.slack.spi.SlackLinkAccessManager;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AbstractAdminServletTest {
    @Mock
    private LoginRedirectionManager loginRedirectionManager;
    @Mock
    private SlackLinkAccessManager slackLinkAccessManager;
    @Mock
    private UserManager userManager;
    @Mock
    private HttpServletRequest servletRequest;
    @Mock
    private HttpServletResponse servletResponse;
    @Mock
    private UserProfile userProfile;
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private AbstractAdminServlet abstractAdminServlet;

    @Before
    public void setUp() {
        abstractAdminServlet = new AbstractAdminServlet(userManager, loginRedirectionManager, slackLinkAccessManager) {
            @Override
            protected void onPermissionError(final HttpServletRequest request, final HttpServletResponse response) {
            }
        };
    }

    @Test
    public void service_shouldRejectUserWhenNotLoggedIn() throws Exception {
        abstractAdminServlet.service(servletRequest, servletResponse);

        verify(loginRedirectionManager).redirectToLogin(servletRequest, servletResponse);
    }

    @Test
    public void service_shouldRejectUserWithNoPermissions() throws IOException, ServletException {
        when(userManager.getRemoteUser()).thenReturn(userProfile);
        when(slackLinkAccessManager.hasAccess(userProfile, servletRequest)).thenReturn(false);

        abstractAdminServlet.service(servletRequest, servletResponse);

        verify(loginRedirectionManager).redirectToLogin(servletRequest, servletResponse);
    }

    @Test
    public void service_shouldAccepUserWithPermissions() throws IOException, ServletException {
        when(userManager.getRemoteUser()).thenReturn(userProfile);
        when(slackLinkAccessManager.hasAccess(userProfile, servletRequest)).thenReturn(true);
        when(servletRequest.getMethod()).thenReturn("get");

        abstractAdminServlet.service(servletRequest, servletResponse);

        verify(loginRedirectionManager, never()).redirectToLogin(servletRequest, servletResponse);
    }
}
