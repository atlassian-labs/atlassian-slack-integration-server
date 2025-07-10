package com.atlassian.plugins.slack.rest;

import com.atlassian.confluence.compat.api.service.accessmode.ReadOnlyAccessAllowed;
import com.atlassian.plugins.slack.rest.model.SlackUserDto;
import com.atlassian.plugins.slack.settings.SlackUserSettingsService;
import com.atlassian.plugins.slack.spi.SlackPluginResourceProvider;
import com.atlassian.plugins.slack.user.SlackUserService;
import com.atlassian.sal.api.user.UserManager;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Optional;

@ReadOnlyAccessAllowed
@Path("/users")
public class SlackUsersResource {
    private final SlackUserService slackUserService;
    private final SlackPluginResourceProvider slackPluginResourceProvider;
    private final SlackUserSettingsService slackUserSettingsService;
    private final UserManager userManager;

    @Inject
    public SlackUsersResource(final SlackUserService slackUserService,
                              final SlackPluginResourceProvider slackPluginResourceProvider,
                              final SlackUserSettingsService slackUserSettingsService,
                              final UserManager userManager) {
        this.slackUserService = slackUserService;
        this.slackPluginResourceProvider = slackPluginResourceProvider;
        this.slackUserSettingsService = slackUserSettingsService;
        this.userManager = userManager;
    }

    @GET
    @Path("/{username}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserInfo(@PathParam("username") final String username) {
        List<SlackUserDto> slackUsers = slackUserService.getSlackUsersByUsername(username);
        return Response.ok(slackUsers).build();
    }

    @PUT
    @Path("/notification")
    public Response saveUserNotificationTeam(@QueryParam("teamId") final String teamId) {
        slackUserSettingsService.setNotificationTeamId(userManager.getRemoteUserKey(), teamId);
        return Response.ok().build();
    }

    @DELETE
    @Path("/notification")
    public Response deleteUserNotificationTeam() {
        slackUserSettingsService.removeNotificationTeamId(userManager.getRemoteUserKey());
        return Response.ok().build();
    }

    @PUT
    @Path("/notification/{key}")
    public Response enableUserNotification(@PathParam("key") final String keyStr) {
        return findNotificationKey(keyStr)
                .map(key -> {
                    slackUserSettingsService.enablePersonalNotificationType(userManager.getRemoteUserKey(), key);
                    return Response.ok().build();
                })
                .orElseGet(() -> Response.status(404).build());
    }

    @DELETE
    @Path("/notification/{key}")
    public Response disableUserNotification(@PathParam("key") final String keyStr) {
        return findNotificationKey(keyStr)
                .map(key -> {
                    slackUserSettingsService.disablePersonalNotificationType(userManager.getRemoteUserKey(), key);
                    return Response.ok().build();
                })
                .orElseGet(() -> Response.status(404).build());
    }

    private Optional<Enum<?>> findNotificationKey(final String key) {
        return slackPluginResourceProvider.getPersonalConfigurationKeys()
                .stream()
                .filter(value -> value.name().toLowerCase().equals(key))
                .findFirst();
    }
}
