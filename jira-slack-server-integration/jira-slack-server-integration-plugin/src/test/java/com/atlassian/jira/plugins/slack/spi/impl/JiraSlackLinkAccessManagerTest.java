package com.atlassian.jira.plugins.slack.spi.impl;

import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.user.UserKey;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;

import static com.atlassian.jira.plugins.slack.spi.impl.JiraConfigurationRedirectionManager.FROM_PROJECT_ATTRIBUTE_KEY;
import static com.atlassian.jira.plugins.slack.spi.impl.JiraConfigurationRedirectionManager.PROJECT_ATTRIBUTE_KEY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JiraSlackLinkAccessManagerTest {
    private static final String PROJECT = "P";
    private static final String USER = "USR";
    private static final UserKey userKey = new UserKey(USER);

    @Mock
    private PermissionManager permissionManager;
    @Mock
    private ProjectManager projectManager;
    @Mock
    private com.atlassian.jira.user.util.UserManager jiraUserManager;
    @Mock
    private UserManager salUserManager;

    @Mock
    private Project project;
    @Mock
    private ApplicationUser applicationUser;
    @Mock
    private UserProfile userProfile;
    @Mock
    private HttpServletRequest httpServletRequest;
    @Mock
    private ContainerRequestContext containerRequest;
    @Mock
    private HttpSession session;
    @Mock
    private MultivaluedMap<String, String> map;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private JiraSlackLinkAccessManager target;

    @Test
    public void hasAccess_containerRequest_grantAccessForAdmin() {
        when(salUserManager.isAdmin(userKey)).thenReturn(true);
        when(userProfile.getUserKey()).thenReturn(userKey);

        boolean result = target.hasAccess(userProfile, containerRequest);

        assertThat(result, is(true));
    }

    @Test
    public void hasAccess_containerRequest_grantAccessForSysAdmin() {
        when(salUserManager.isAdmin(userKey)).thenReturn(false);
        when(salUserManager.isSystemAdmin(userKey)).thenReturn(true);
        when(userProfile.getUserKey()).thenReturn(userKey);

        boolean result = target.hasAccess(userProfile, containerRequest);

        assertThat(result, is(true));
    }

    @Test
    public void hasAccess_containerRequest_grantAccessForProjectAdmin() {
        when(salUserManager.isAdmin(userKey)).thenReturn(false);
        when(salUserManager.isSystemAdmin(userKey)).thenReturn(false);
        when(userProfile.getUserKey()).thenReturn(userKey);
        UriInfo mockUriInfo = mock(UriInfo.class);
        when(containerRequest.getUriInfo()).thenReturn(mockUriInfo);
        when(mockUriInfo.getQueryParameters()).thenReturn(map);
        when(map.getFirst("projectKey")).thenReturn(PROJECT);
        when(projectManager.getProjectByCurrentKey(PROJECT)).thenReturn(project);
        when(jiraUserManager.getUserByKeyEvenWhenUnknown(USER)).thenReturn(applicationUser);
        when(permissionManager.hasPermission(ProjectPermissions.ADMINISTER_PROJECTS, project, applicationUser)).thenReturn(true);

        boolean result = target.hasAccess(userProfile, containerRequest);

        assertThat(result, is(true));
    }

    @Test
    public void hasAccess_containerRequest_notGrantAccessForNonProjectAdmin() {
        when(salUserManager.isAdmin(userKey)).thenReturn(false);
        when(salUserManager.isSystemAdmin(userKey)).thenReturn(false);
        when(userProfile.getUserKey()).thenReturn(userKey);
        UriInfo mockUriInfo = mock(UriInfo.class);
        when(containerRequest.getUriInfo()).thenReturn(mockUriInfo);
        when(mockUriInfo.getQueryParameters()).thenReturn(map);
        when(map.getFirst("projectKey")).thenReturn(PROJECT);
        when(projectManager.getProjectByCurrentKey(PROJECT)).thenReturn(project);
        when(jiraUserManager.getUserByKeyEvenWhenUnknown(USER)).thenReturn(applicationUser);
        when(permissionManager.hasPermission(ProjectPermissions.ADMINISTER_PROJECTS, project, applicationUser)).thenReturn(false);

        boolean result = target.hasAccess(userProfile, containerRequest);

        assertThat(result, is(false));
    }

    @Test
    public void hasAccess_containerRequest_notGrantAccessForNonExistingProject() {
        when(salUserManager.isAdmin(userKey)).thenReturn(false);
        when(salUserManager.isSystemAdmin(userKey)).thenReturn(false);
        when(userProfile.getUserKey()).thenReturn(userKey);
        UriInfo mockUriInfo = mock(UriInfo.class);
        when(containerRequest.getUriInfo()).thenReturn(mockUriInfo);
        when(mockUriInfo.getQueryParameters()).thenReturn(map);
        when(map.getFirst("projectKey")).thenReturn(PROJECT);
        when(projectManager.getProjectByCurrentKey(PROJECT)).thenReturn(null);

        boolean result = target.hasAccess(userProfile, containerRequest);

        assertThat(result, is(false));
    }

    @Test
    public void hasAccess_servletRequest_grantAccessForAdmin() {
        when(salUserManager.isAdmin(userKey)).thenReturn(true);
        when(userProfile.getUserKey()).thenReturn(userKey);

        boolean result = target.hasAccess(userProfile, httpServletRequest);

        assertThat(result, is(true));
    }

    @Test
    public void hasAccess_servletRequest_grantAccessForSysAdmin() {
        when(salUserManager.isAdmin(userKey)).thenReturn(false);
        when(salUserManager.isSystemAdmin(userKey)).thenReturn(true);
        when(userProfile.getUserKey()).thenReturn(userKey);

        boolean result = target.hasAccess(userProfile, httpServletRequest);

        assertThat(result, is(true));
    }

    @Test
    public void hasAccess_servletRequest_grantAccessForProjectAdmin() {
        when(salUserManager.isAdmin(userKey)).thenReturn(false);
        when(salUserManager.isSystemAdmin(userKey)).thenReturn(false);
        when(userProfile.getUserKey()).thenReturn(userKey);
        when(httpServletRequest.getParameter("projectKey")).thenReturn(PROJECT);
        when(projectManager.getProjectByCurrentKey(PROJECT)).thenReturn(project);
        when(jiraUserManager.getUserByKeyEvenWhenUnknown(USER)).thenReturn(applicationUser);
        when(permissionManager.hasPermission(ProjectPermissions.ADMINISTER_PROJECTS, project, applicationUser)).thenReturn(true);

        boolean result = target.hasAccess(userProfile, httpServletRequest);

        assertThat(result, is(true));
    }

    @Test
    public void hasAccess_servletRequest_notGrantAccessForNonProjectAdmin() {
        when(salUserManager.isAdmin(userKey)).thenReturn(false);
        when(salUserManager.isSystemAdmin(userKey)).thenReturn(false);
        when(userProfile.getUserKey()).thenReturn(userKey);
        when(httpServletRequest.getParameter("projectKey")).thenReturn(PROJECT);
        when(projectManager.getProjectByCurrentKey(PROJECT)).thenReturn(project);
        when(jiraUserManager.getUserByKeyEvenWhenUnknown(USER)).thenReturn(applicationUser);
        when(permissionManager.hasPermission(ProjectPermissions.ADMINISTER_PROJECTS, project, applicationUser)).thenReturn(false);

        boolean result = target.hasAccess(userProfile, httpServletRequest);

        assertThat(result, is(false));
    }

    @Test
    public void hasAccess_servletRequest_notGrantAccessForNonExistingProject() {
        when(salUserManager.isAdmin(userKey)).thenReturn(false);
        when(salUserManager.isSystemAdmin(userKey)).thenReturn(false);
        when(userProfile.getUserKey()).thenReturn(userKey);
        when(httpServletRequest.getParameter("projectKey")).thenReturn(PROJECT);
        when(projectManager.getProjectByCurrentKey(PROJECT)).thenReturn(null);

        boolean result = target.hasAccess(userProfile, httpServletRequest);

        assertThat(result, is(false));
    }

    @Test
    public void hasAccess_servletRequest_grantAccessForProjectKeyInSession() {
        when(salUserManager.isAdmin(userKey)).thenReturn(false);
        when(salUserManager.isSystemAdmin(userKey)).thenReturn(false);
        when(userProfile.getUserKey()).thenReturn(userKey);
        when(httpServletRequest.getParameter("projectKey")).thenReturn(null);
        when(httpServletRequest.getSession()).thenReturn(session);
        when(session.getAttribute(FROM_PROJECT_ATTRIBUTE_KEY)).thenReturn(true);
        when(session.getAttribute(PROJECT_ATTRIBUTE_KEY)).thenReturn(PROJECT);
        when(projectManager.getProjectByCurrentKey(PROJECT)).thenReturn(project);
        when(jiraUserManager.getUserByKeyEvenWhenUnknown(USER)).thenReturn(applicationUser);
        when(permissionManager.hasPermission(ProjectPermissions.ADMINISTER_PROJECTS, project, applicationUser)).thenReturn(true);

        boolean result = target.hasAccess(userProfile, httpServletRequest);

        assertThat(result, is(true));
    }
}
