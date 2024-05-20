package com.atlassian.confluence.plugins.slack.spacetochannel.rest;

import com.atlassian.confluence.plugins.slack.spacetochannel.service.SpacesWithAdminPermissionProvider;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/spaces")
public class SpaceToChannelSpaceResource {
    static final int MAX_RESULTS_SIZE = 75;
    private static final int MINIMUM_QUERY_LENGTH = 1;

    private final SpacesWithAdminPermissionProvider defaultSpacesWithAdminPermissionProvider;

    @Inject
    public SpaceToChannelSpaceResource(final SpacesWithAdminPermissionProvider defaultSpacesWithAdminPermissionProvider) {
        this.defaultSpacesWithAdminPermissionProvider = defaultSpacesWithAdminPermissionProvider;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSpaces(@QueryParam("name") final String name) {
        final ConfluenceUser user = AuthenticatedUserThreadLocal.get();

        if (StringUtils.isBlank(name) || name.length() < MINIMUM_QUERY_LENGTH) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Parameter 'name' must have at least 2 characters")
                    .build();
        }

        return defaultSpacesWithAdminPermissionProvider.findSpacesMatchingName(name, user, MAX_RESULTS_SIZE).fold(
                e -> Response.serverError().entity(e.getMessage()).build(),
                spaces -> Response.ok(spaces).build());
    }
}
