package com.atlassian.plugins.slack.rest;

import com.atlassian.plugins.slack.spi.SlackLinkAccessManager;
import com.atlassian.sal.api.user.UserManager;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

/**
 * Determines whether the user has permission to uninstall an existing Slack link.
 * If not link id is provided, the default link is used.
 */
@Provider
public class SlackLinkUninstallPermissionResourceFilter implements ResourceFilter, ContainerRequestFilter {
    @Context
    private UriInfo uriInfo;

    private final UserManager userManager;
    private final SlackLinkAccessManager slackLinkAccessManager;

    public SlackLinkUninstallPermissionResourceFilter(@Qualifier("salUserManager") final UserManager userManager,
                                                      final SlackLinkAccessManager slackLinkAccessManager) {
        this.userManager = userManager;
        this.slackLinkAccessManager = slackLinkAccessManager;
    }

    @Override
    public ContainerRequest filter(final ContainerRequest containerRequest) {
        if (!hasAccess(containerRequest)) {
            throw new SecurityException("User must be an Administrator to uninstall this plugin.");
        }

        return containerRequest;
    }

    private boolean hasAccess(final ContainerRequest containerRequest) {
        return slackLinkAccessManager.hasAccess(userManager.getRemoteUser(), containerRequest);
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
