package com.atlassian.plugins.slack.rest;

import com.atlassian.plugins.slack.spi.SlackLinkAccessManager;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;

/**
 * Determines whether the user has permission to administer Slack links.
 */
@Provider
@Component
@SlackLinkAdministerPermission
public class SlackLinkAdministerPermissionResourceFilter implements ContainerRequestFilter {
    private final UserManager userManager;
    private final SlackLinkAccessManager slackLinkAccessManager;

    @Inject
    @Autowired
    public SlackLinkAdministerPermissionResourceFilter(
            @Qualifier("salUserManager") final UserManager userManager,
            final SlackLinkAccessManager slackLinkAccessManager) {
        this.userManager = userManager;
        this.slackLinkAccessManager = slackLinkAccessManager;
    }

    @Override
    public void filter(final ContainerRequestContext containerRequest) {
        if (!hasAccess(containerRequest)) {
            throw new SecurityException("User must be an Admin to configure this plugin.");
        }
    }

    private boolean hasAccess(final ContainerRequestContext containerRequest) {
        UserProfile remoteUser = userManager.getRemoteUser();
        return slackLinkAccessManager.hasAccess(remoteUser, containerRequest);
    }
}
