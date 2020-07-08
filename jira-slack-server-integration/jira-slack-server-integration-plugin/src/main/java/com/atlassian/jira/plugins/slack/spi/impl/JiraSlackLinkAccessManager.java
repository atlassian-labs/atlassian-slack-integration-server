package com.atlassian.jira.plugins.slack.spi.impl;

import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugins.slack.spi.SlackLinkAccessManager;
import com.atlassian.plugins.slack.spi.impl.AbstractSlackLinkAccessManager;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import com.sun.jersey.spi.container.ContainerRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
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

    private boolean hasAccess(final UserProfile userProfile, final Optional<Project> project) {
        return project
                .filter(project1 -> isProjectAdmin(getUserByProfile(userProfile), project1))
                .isPresent();

    }

    @Override
    public boolean hasAccess(final UserProfile userProfile, final ContainerRequest request) {
        if (super.hasAccess(userProfile, request)) {
            return true;
        }

        return hasAccess(userProfile, getProject(request));
    }

    @Override
    public boolean hasAccess(final UserProfile userProfile, final HttpServletRequest request) {
        if (super.hasAccess(userProfile, request)) {
            return true;
        }

        return hasAccess(userProfile, getProject(request));
    }

    private Optional<Project> getProject(final ContainerRequest request) {
        final MultivaluedMap<String, String> params = request.getQueryParameters();
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

    private ApplicationUser getUserByProfile(final UserProfile userProfile) {
        return jiraUserManager.getUserByKeyEvenWhenUnknown(userProfile.getUserKey().getStringValue());
    }
}
