package com.atlassian.plugins.slack.rest;

import com.atlassian.plugins.slack.spi.SlackLinkAccessManager;
import com.atlassian.sal.api.user.UserManager;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

/**
 * Determines whether the user has permission to uninstall an existing Slack link.
 * If not link id is provided, the default link is used.
 */
@Provider
public class SlackLinkUninstallPermissionResourceFilter implements ContainerRequestFilter {
    @Context
    private UriInfo uriInfo;

    private final UserManager userManager;
    private final SlackLinkAccessManager slackLinkAccessManager;

    @Inject
    public SlackLinkUninstallPermissionResourceFilter(@Qualifier("salUserManager") final UserManager userManager,
                                                      final SlackLinkAccessManager slackLinkAccessManager) {
        this.userManager = userManager;
        this.slackLinkAccessManager = slackLinkAccessManager;
    }

    @Override
    public void filter(final ContainerRequestContext containerRequest) {
        if (!hasAccess(containerRequest)) {
            throw new SecurityException("User must be an Administrator to uninstall this plugin.");
        }
    }

    private boolean hasAccess(final ContainerRequestContext containerRequest) {
        return slackLinkAccessManager.hasAccess(userManager.getRemoteUser(), containerRequest);
    }
}
