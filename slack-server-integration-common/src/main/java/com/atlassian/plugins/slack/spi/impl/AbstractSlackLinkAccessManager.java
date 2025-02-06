package com.atlassian.plugins.slack.spi.impl;

import com.atlassian.plugins.slack.spi.SlackLinkAccessManager;
import com.atlassian.sal.api.user.UserKey;
import com.atlassian.sal.api.user.UserManager;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;

public abstract class AbstractSlackLinkAccessManager implements SlackLinkAccessManager {

    private final UserManager userManager;

    public AbstractSlackLinkAccessManager(final UserManager userManager) {
        this.userManager = userManager;
    }

    @Override
    public boolean hasAccess(UserKey userKey, ContainerRequestContext request) {
        return hasAccess(userKey);
    }

    @Override
    public boolean hasAccess(UserKey userKey, HttpServletRequest request) {
        return hasAccess(userKey);
    }

    @Override
    public boolean isSiteAdmin(UserKey userKey) {
        if (userKey == null) {
            return false;
        }

        return (userManager.isAdmin(userKey) || userManager.isSystemAdmin(userKey));
    }

    private boolean hasAccess(UserKey userKey) {
        return isSiteAdmin(userKey);
    }
}
