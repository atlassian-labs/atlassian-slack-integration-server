package com.atlassian.jira.plugins.slack.spi.impl;

import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugins.slack.spi.SlackLinkAccessManager;
import com.atlassian.plugins.slack.spi.impl.AbstractSlackLinkAccessManager;
import com.atlassian.sal.api.user.UserKey;
import com.atlassian.sal.api.user.UserManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedMap;
import java.util.Optional;

import static com.atlassian.jira.plugins.slack.spi.impl.JiraConfigurationRedirectionManager.FROM_PROJECT_ATTRIBUTE_KEY;
import static com.atlassian.jira.plugins.slack.spi.impl.JiraConfigurationRedirectionManager.PROJECT_ATTRIBUTE_KEY;

@Component
public class JiraSlackLinkAccessManager extends AbstractSlackLinkAccessManager implements SlackLinkAccessManager {
    private final PermissionManager permissionManager;
    private final ProjectManager projectManager;
    private final com.atlassian.jira.user.util.UserManager jiraUserManager;

    @Autowired
    public JiraSlackLinkAccessManager(
            @Qualifier("salUserManager") final UserManager userManager,
            final PermissionManager permissionManager,
            final ProjectManager projectManager,
            @Qualifier("jiraUserManager") final com.atlassian.jira.user.util.UserManager jiraUserManager) {
        super(userManager);
        this.permissionManager = permissionManager;
        this.projectManager = projectManager;
        this.jiraUserManager = jiraUserManager;
    }

    private boolean hasAccess(final UserKey userKey, final Optional<Project> project) {
        return project
                .filter(project1 -> isProjectAdmin(getUserByProfile(userKey), project1))
                .isPresent();

    }

    @Override
    public boolean hasAccess(final UserKey userKey, final ContainerRequestContext request) {
        if (super.hasAccess(userKey, request)) {
            return true;
        }

        return hasAccess(userKey, getProject(request));
    }

    @Override
    public boolean hasAccess(final UserKey userKey, final HttpServletRequest request) {
        if (super.hasAccess(userKey, request)) {
            return true;
        }

        return hasAccess(userKey, getProject(request));
    }

    private Optional<Project> getProject(final ContainerRequestContext request) {
        final MultivaluedMap<String, String> params = request.getUriInfo().getQueryParameters();
        return getProjectFromKey(params.getFirst("projectKey"));
    }

    private Optional<Project> getProject(final HttpServletRequest request) {
        String projectKey = request.getParameter("projectKey");

        if (projectKey == null) {
            HttpSession session = request.getSession();
            final Boolean sentFromProject = (Boolean) session.getAttribute(FROM_PROJECT_ATTRIBUTE_KEY);
            if (sentFromProject != null && sentFromProject) {
                projectKey = (String) session.getAttribute(PROJECT_ATTRIBUTE_KEY);
            }
        }

        return getProjectFromKey(projectKey);
    }

    private Optional<Project> getProjectFromKey(final String projectKey) {
        return Optional.ofNullable(projectKey).map(projectManager::getProjectByCurrentKey);
    }

    private boolean isProjectAdmin(final ApplicationUser user, final Project project) {
        return permissionManager.hasPermission(ProjectPermissions.ADMINISTER_PROJECTS, project, user);
    }

    private ApplicationUser getUserByProfile(final UserKey userKey) {
        return jiraUserManager.getUserByKeyEvenWhenUnknown(userKey.getStringValue());
    }
}
