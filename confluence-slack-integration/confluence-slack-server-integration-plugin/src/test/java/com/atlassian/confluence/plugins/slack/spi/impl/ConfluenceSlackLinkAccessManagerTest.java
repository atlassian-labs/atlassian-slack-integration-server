package com.atlassian.confluence.plugins.slack.spi.impl;

import com.atlassian.confluence.security.Permission;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.spaces.SpaceManager;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.sal.api.user.UserKey;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import com.sun.jersey.spi.container.ContainerRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.MultivaluedMap;

import static com.atlassian.confluence.plugins.slack.spi.impl.ConfluenceConfigurationRedirectionManager.FROM_SPACE_ATTRIBUTE_KEY;
import static com.atlassian.confluence.plugins.slack.spi.impl.ConfluenceConfigurationRedirectionManager.SPACE_ATTRIBUTE_KEY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ConfluenceSlackLinkAccessManagerTest {
    private static final String SPACE_KEY = "SK";
    private static final String USER = "USR";
    private static final UserKey userKey = new UserKey(USER);

    @Mock
    private UserManager userManager;
    @Mock
    private PermissionManager permissionManager;
    @Mock
    private SpaceManager spaceManager;
    @Mock
    private UserAccessor userAccessor;

    @Mock
    private UserProfile userProfile;
    @Mock
    private ConfluenceUser user;
    @Mock
    private Space space;
    @Mock
    private HttpServletRequest httpServletRequest;
    @Mock
    private ContainerRequest containerRequest;
    @Mock
    private HttpSession session;
    @Mock
    private MultivaluedMap<String, String> map;

    @InjectMocks
    private ConfluenceSlackLinkAccessManager target;

    @Test
    public void hasAccess_containerRequest_grantAccessForAdmin() {
        when(userManager.isAdmin(userKey)).thenReturn(true);
        when(userProfile.getUserKey()).thenReturn(userKey);

        boolean result = target.hasAccess(userProfile, containerRequest);

        assertThat(result, is(true));
    }

    @Test
    public void hasAccess_containerRequest_grantAccessForSysAdmin() {
        when(userManager.isAdmin(userKey)).thenReturn(false);
        when(userManager.isSystemAdmin(userKey)).thenReturn(true);
        when(userProfile.getUserKey()).thenReturn(userKey);

        boolean result = target.hasAccess(userProfile, containerRequest);

        assertThat(result, is(true));
    }

    @Test
    public void hasAccess_containerRequest_grantAccessForSpaceAdmin() {
        when(userManager.isAdmin(userKey)).thenReturn(false);
        when(userManager.isSystemAdmin(userKey)).thenReturn(false);
        when(userProfile.getUserKey()).thenReturn(userKey);
        when(containerRequest.getQueryParameters()).thenReturn(map);
        when(map.getFirst("key")).thenReturn(SPACE_KEY);
        when(spaceManager.getSpace(SPACE_KEY)).thenReturn(space);
        when(userAccessor.getExistingUserByKey(userKey)).thenReturn(user);
        when(permissionManager.hasPermission(user, Permission.ADMINISTER, space)).thenReturn(true);

        boolean result = target.hasAccess(userProfile, containerRequest);

        assertThat(result, is(true));
    }

    @Test
    public void hasAccess_containerRequest_notGrantAccessForNonSpaceAdmin() {
        when(userManager.isAdmin(userKey)).thenReturn(false);
        when(userManager.isSystemAdmin(userKey)).thenReturn(false);
        when(userProfile.getUserKey()).thenReturn(userKey);
        when(containerRequest.getQueryParameters()).thenReturn(map);
        when(map.getFirst("key")).thenReturn(SPACE_KEY);
        when(spaceManager.getSpace(SPACE_KEY)).thenReturn(space);
        when(userAccessor.getExistingUserByKey(userKey)).thenReturn(user);
        when(permissionManager.hasPermission(user, Permission.ADMINISTER, space)).thenReturn(false);

        boolean result = target.hasAccess(userProfile, containerRequest);

        assertThat(result, is(false));
    }

    @Test
    public void hasAccess_containerRequest_notGrantAccessForNonExistingSpace() {
        when(userManager.isAdmin(userKey)).thenReturn(false);
        when(userManager.isSystemAdmin(userKey)).thenReturn(false);
        when(userProfile.getUserKey()).thenReturn(userKey);
        when(containerRequest.getQueryParameters()).thenReturn(map);
        when(map.getFirst("key")).thenReturn(SPACE_KEY);
        when(spaceManager.getSpace(SPACE_KEY)).thenReturn(null);

        boolean result = target.hasAccess(userProfile, containerRequest);

        assertThat(result, is(false));
    }

    @Test
    public void hasAccess_servletRequest_grantAccessForAdmin() {
        when(userManager.isAdmin(userKey)).thenReturn(true);
        when(userProfile.getUserKey()).thenReturn(userKey);

        boolean result = target.hasAccess(userProfile, httpServletRequest);

        assertThat(result, is(true));
    }

    @Test
    public void hasAccess_servletRequest_grantAccessForSysAdmin() {
        when(userManager.isAdmin(userKey)).thenReturn(false);
        when(userManager.isSystemAdmin(userKey)).thenReturn(true);
        when(userProfile.getUserKey()).thenReturn(userKey);

        boolean result = target.hasAccess(userProfile, httpServletRequest);

        assertThat(result, is(true));
    }

    @Test
    public void hasAccess_servletRequest_grantAccessForSpaceAdmin() {
        when(userManager.isAdmin(userKey)).thenReturn(false);
        when(userManager.isSystemAdmin(userKey)).thenReturn(false);
        when(userProfile.getUserKey()).thenReturn(userKey);
        when(httpServletRequest.getParameter("key")).thenReturn(SPACE_KEY);
        when(spaceManager.getSpace(SPACE_KEY)).thenReturn(space);
        when(userAccessor.getExistingUserByKey(userKey)).thenReturn(user);
        when(permissionManager.hasPermission(user, Permission.ADMINISTER, space)).thenReturn(true);

        boolean result = target.hasAccess(userProfile, httpServletRequest);

        assertThat(result, is(true));
    }

    @Test
    public void hasAccess_servletRequest_notGrantAccessForNonSpaceAdmin() {
        when(userManager.isAdmin(userKey)).thenReturn(false);
        when(userManager.isSystemAdmin(userKey)).thenReturn(false);
        when(userProfile.getUserKey()).thenReturn(userKey);
        when(httpServletRequest.getParameter("key")).thenReturn(SPACE_KEY);
        when(spaceManager.getSpace(SPACE_KEY)).thenReturn(space);
        when(userAccessor.getExistingUserByKey(userKey)).thenReturn(user);
        when(permissionManager.hasPermission(user, Permission.ADMINISTER, space)).thenReturn(false);

        boolean result = target.hasAccess(userProfile, httpServletRequest);

        assertThat(result, is(false));
    }

    @Test
    public void hasAccess_servletRequest_notGrantAccessForNonExistingSpace() {
        when(userManager.isAdmin(userKey)).thenReturn(false);
        when(userManager.isSystemAdmin(userKey)).thenReturn(false);
        when(userProfile.getUserKey()).thenReturn(userKey);
        when(httpServletRequest.getParameter("key")).thenReturn(SPACE_KEY);
        when(spaceManager.getSpace(SPACE_KEY)).thenReturn(null);

        boolean result = target.hasAccess(userProfile, httpServletRequest);

        assertThat(result, is(false));
    }

    @Test
    public void hasAccess_servletRequest_grantAccessForSpaceKeyInSession() {
        when(userManager.isAdmin(userKey)).thenReturn(false);
        when(userManager.isSystemAdmin(userKey)).thenReturn(false);
        when(userProfile.getUserKey()).thenReturn(userKey);
        when(httpServletRequest.getParameter("key")).thenReturn(null);
        when(httpServletRequest.getSession()).thenReturn(session);
        when(session.getAttribute(FROM_SPACE_ATTRIBUTE_KEY)).thenReturn(true);
        when(session.getAttribute(SPACE_ATTRIBUTE_KEY)).thenReturn(SPACE_KEY);
        when(spaceManager.getSpace(SPACE_KEY)).thenReturn(space);
        when(userAccessor.getExistingUserByKey(userKey)).thenReturn(user);
        when(permissionManager.hasPermission(user, Permission.ADMINISTER, space)).thenReturn(true);

        boolean result = target.hasAccess(userProfile, httpServletRequest);

        assertThat(result, is(true));
    }
}
