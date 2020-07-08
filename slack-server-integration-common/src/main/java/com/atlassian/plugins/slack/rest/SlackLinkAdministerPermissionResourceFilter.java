package com.atlassian.plugins.slack.rest;

import com.atlassian.plugins.slack.spi.SlackLinkAccessManager;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.ws.rs.ext.Provider;

/**
 * Determines whether the user has permission to administer Slack links.
 */
@Provider
public class SlackLinkAdministerPermissionResourceFilter implements ResourceFilter, ContainerRequestFilter {
    private final UserManager userManager;
    private final SlackLinkAccessManager slackLinkAccessManager;

    @Autowired
    public SlackLinkAdministerPermissionResourceFilter(
            @Qualifier("salUserManager") final UserManager userManager,
            final SlackLinkAccessManager slackLinkAccessManager) {
        this.userManager = userManager;
        this.slackLinkAccessManager = slackLinkAccessManager;
    }

    @Override
    public ContainerRequest filter(final ContainerRequest containerRequest) {
        if (hasAccess(containerRequest)) {
            return containerRequest;
        }

        throw new SecurityException("User must be an Admin to configure this plugin.");
    }

    private boolean hasAccess(final ContainerRequest containerRequest) {
        UserProfile remoteUser = userManager.getRemoteUser();
        return slackLinkAccessManager.hasAccess(remoteUser, containerRequest);
    }

    @Override
    public ContainerRequestFilter getRequestFilter() {
        return this;
    }

    @Override
    public ContainerResponseFilter getResponseFilter() {
        return null;
    }
}
