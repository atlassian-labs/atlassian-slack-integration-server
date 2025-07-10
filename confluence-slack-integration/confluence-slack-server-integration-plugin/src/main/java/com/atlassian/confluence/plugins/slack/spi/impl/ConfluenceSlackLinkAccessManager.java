package com.atlassian.confluence.plugins.slack.spi.impl;

import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.spaces.SpaceManager;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.plugins.slack.spi.impl.AbstractSlackLinkAccessManager;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MultivaluedMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.atlassian.confluence.plugins.slack.spi.impl.ConfluenceConfigurationRedirectionManager.FROM_SPACE_ATTRIBUTE_KEY;
import static com.atlassian.confluence.plugins.slack.spi.impl.ConfluenceConfigurationRedirectionManager.SPACE_ATTRIBUTE_KEY;
import static com.atlassian.confluence.security.Permission.ADMINISTER;

@Component
public class ConfluenceSlackLinkAccessManager extends AbstractSlackLinkAccessManager {

    private final PermissionManager permissionManager;
    private final SpaceManager spaceManager;
    private final UserAccessor userAccessor;

    @Autowired
    public ConfluenceSlackLinkAccessManager(@Qualifier("salUserManager") final UserManager userManager,
                                            final PermissionManager permissionManager,
                                            final SpaceManager spaceManager,
                                            final UserAccessor userAccessor) {
        super(userManager);
        this.permissionManager = permissionManager;
        this.spaceManager = spaceManager;
        this.userAccessor = userAccessor;
    }

    private boolean hasAccess(UserProfile userProfile, Optional<Space> space) {
        if (space.isEmpty()) {
            return false;
        }

        ConfluenceUser user = getUserByProfile(userProfile);
        return isSpaceAdmin(user, space.get());
    }

    @Override
    public boolean hasAccess(UserProfile userProfile, ContainerRequestContext request) {
        if (super.hasAccess(userProfile, request)) {
            return true;
        }

        Optional<Space> space = getSpace(request);

        return hasAccess(userProfile, space);
    }

    @Override
    public boolean hasAccess(UserProfile userProfile, HttpServletRequest request) {
        if (super.hasAccess(userProfile, request)) {
            return true;
        }
        Optional<Space> space = getSpace(request);

        return hasAccess(userProfile, space);
    }

    private Optional<Space> getSpace(ContainerRequestContext request) {
        MultivaluedMap<String, String> params = request.getUriInfo().getQueryParameters();
        String spaceKey = params.getFirst("key");

        return getSpaceFromKey(spaceKey);
    }

    private Optional<Space> getSpace(HttpServletRequest request) {
        String spaceKey = request.getParameter("key");

        if (spaceKey == null) {
            HttpSession session = request.getSession();
            final Boolean sentFromSpace = (Boolean) session.getAttribute(FROM_SPACE_ATTRIBUTE_KEY);
            if (sentFromSpace != null && sentFromSpace) {
                spaceKey = (String) session.getAttribute(SPACE_ATTRIBUTE_KEY);
            }
        }

        return getSpaceFromKey(spaceKey);
    }

    private Optional<Space> getSpaceFromKey(String spaceKey) {
        if (spaceKey == null) {
            return Optional.empty();
        }

        final Space space = spaceManager.getSpace(spaceKey);
        return Optional.ofNullable(space);
    }

    private boolean isSpaceAdmin(ConfluenceUser user, Space space) {
        return permissionManager.hasPermission(user, ADMINISTER, space);
    }

    private ConfluenceUser getUserByProfile(UserProfile userProfile) {
        return userAccessor.getExistingUserByKey(userProfile.getUserKey());
    }
}
