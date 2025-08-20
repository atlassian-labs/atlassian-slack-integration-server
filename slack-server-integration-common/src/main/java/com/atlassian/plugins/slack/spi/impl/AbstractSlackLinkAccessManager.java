package com.atlassian.plugins.slack.spi.impl;

import com.atlassian.plugins.slack.spi.SlackLinkAccessManager;
import com.atlassian.sal.api.user.UserKey;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ContainerRequestContext;

public abstract class AbstractSlackLinkAccessManager implements SlackLinkAccessManager {

    private final UserManager userManager;

    public AbstractSlackLinkAccessManager(final UserManager userManager) {
        this.userManager = userManager;
    }

    @Override
    public boolean hasAccess(UserProfile userProfile, ContainerRequestContext request) {
        return hasAccess(userProfile);
    }

    @Override
    public boolean hasAccess(UserProfile userProfile, HttpServletRequest request) {
        return hasAccess(userProfile);
    }

    @Override
    public boolean isSiteAdmin(UserProfile userProfile) {
        if (userProfile == null) {
            return false;
        }

        UserKey userKey = userProfile.getUserKey();
        return (userManager.isAdmin(userKey) || userManager.isSystemAdmin(userKey));
    }

    private boolean hasAccess(UserProfile userProfile) {
        return isSiteAdmin(userProfile);
    }
}
