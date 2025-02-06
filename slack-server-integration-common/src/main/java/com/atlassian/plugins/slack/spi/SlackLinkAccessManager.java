package com.atlassian.plugins.slack.spi;

import com.atlassian.sal.api.user.UserKey;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;

/**
 * Determines if a user has access to the Slack link.
 */
public interface SlackLinkAccessManager {
    /**
     * Returns true if the given user has access to add a link
     * given the request.
     *
     * @param userKey - the key of logged-in user
     * @param request - the request
     * @return true if the given user has access to add a link given the request.
     */
    boolean hasAccess(UserKey userKey, ContainerRequestContext request);

    /**
     * Returns true if the given user has access to add a link
     * given the request.
     *
     * @param userKey - the key of logged-in user
     * @param request - the request
     * @return true if the given user has access to add a link given the request.
     */
    boolean hasAccess(UserKey userKey, HttpServletRequest request);

    /**
     * Returns true if the given user is a site administrator
     *
     * @param userKey - the key of logged-in user
     * @return true if the given user is a site administrator
     */
    boolean isSiteAdmin(UserKey userKey);
}
