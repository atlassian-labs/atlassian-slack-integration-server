package com.atlassian.confluence.plugins.slack.spi.impl;

import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.spaces.SpaceManager;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.plugins.slack.spi.impl.AbstractSlackLinkAccessManager;
import com.atlassian.sal.api.user.UserKey;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedMap;
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

    private boolean hasAccess(UserKey userKey, Optional<Space> space) {
        if (!space.isPresent()) {
            return false;
        }

        User user = getUserByProfile(userKey);
        return isSpaceAdmin(user, space.get());
    }

    @Override
    public boolean hasAccess(UserKey userKey, ContainerRequestContext request) {
        if (super.hasAccess(userKey, request)) {
            return true;
        }

        Optional<Space> space = getSpace(request);

        return hasAccess(userKey, space);
    }

    @Override
    public boolean hasAccess(UserKey userKey, HttpServletRequest request) {
        if (super.hasAccess(userKey, request)) {
            return true;
        }
        Optional<Space> space = getSpace(request);

        return hasAccess(userKey, space);
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

    private boolean isSpaceAdmin(User user, Space space) {
        return permissionManager.hasPermission(user, ADMINISTER, space);
    }

    private User getUserByProfile(UserKey userKey) {
        return userAccessor.getExistingUserByKey(userKey);
    }
}
