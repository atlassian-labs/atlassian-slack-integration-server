package com.atlassian.plugins.slack.rest;

import com.atlassian.confluence.compat.api.service.accessmode.ReadOnlyAccessAllowed;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.SlackUser;
import com.atlassian.plugins.slack.api.client.SlackClient;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.plugins.slack.rest.model.DisconnectedTokensDto;
import com.atlassian.plugins.slack.rest.model.DismissUserDisconnectionDto;
import com.atlassian.plugins.slack.settings.SlackSettingService;
import com.atlassian.plugins.slack.spi.SlackLinkAccessManager;
import com.atlassian.plugins.slack.user.SlackUserManager;
import com.atlassian.plugins.slack.util.ErrorResponse;
import com.atlassian.sal.api.message.I18nResolver;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.seratch.jslack.api.methods.response.auth.AuthTestResponse;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

@ReadOnlyAccessAllowed
@Path("/connection-status")
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
public class ConnectionStatusResource {
    private final SlackClientProvider slackClientProvider;
    private final I18nResolver i18nResolver;
    private final SlackLinkManager slackLinkManager;
    private final SlackUserManager slackUserManager;
    private final UserManager userManager;
    private final SlackLinkAccessManager slackLinkAccessManager;
    private final SlackSettingService slackSettingService;

    @Inject
    public ConnectionStatusResource(final SlackClientProvider slackClientProvider,
                                    final I18nResolver i18nResolver,
                                    final SlackLinkManager slackLinkManager,
                                    final SlackUserManager slackUserManager,
                                    final @ComponentImport("salUserManager") UserManager userManager,
                                    final SlackLinkAccessManager slackLinkAccessManager,
                                    final SlackSettingService slackSettingService) {
        this.slackClientProvider = slackClientProvider;
        this.i18nResolver = i18nResolver;
        this.slackLinkManager = slackLinkManager;
        this.slackUserManager = slackUserManager;
        this.userManager = userManager;
        this.slackLinkAccessManager = slackLinkAccessManager;
        this.slackSettingService = slackSettingService;
    }

    @GET
    @Path("/{teamId}")
    public Response getStatus(@PathParam("teamId") final String teamId) {
        return slackClientProvider.withTeamId(teamId).fold(
                e -> Response
                        .status(401)
                        .entity(new Error("UNKNOWN", i18nResolver.getText("plugins.slack.admin.connection.status.invalid.link.error")))
                        .build(),
                client -> client.testToken().fold(
                        this::errorResponse,
                        r -> {
                            boolean isInstancePublic = slackSettingService.isInstancePublic();
                            ConnectionStatus status = isInstancePublic
                                    ? ConnectionStatus.CONNECTED
                                    : ConnectionStatus.PARTIALLY_CONNECTED;
                            return Response.ok(new ConnectionStatusBean(status)).build();
                        })
        );
    }

    @GET
    @Path("/{teamId}/user")
    public Response getUserStatus(@PathParam("teamId") final String teamId) {
        return slackClientProvider.withTeamId(teamId).toOptional()
                .flatMap(SlackClient::withRemoteUserTokenIfAvailable)
                .map(client -> client.testToken().fold(this::errorResponse, r -> Response.ok().build()))
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    private Response errorResponse(final ErrorResponse e) {
        return e.getApiResponse(AuthTestResponse.class).fold(
                ex -> Response
                        .status(502)
                        .entity(new Error("NO_CONNECTION", i18nResolver.getText("plugins.slack.admin.connection.status.connection.error")))
                        .build(),
                api -> Response
                        .status(401)
                        .entity(new Error("OAUTH_FAILURE", i18nResolver.getText("plugins.slack.admin.connection.status.error", api.getError())))
                        .build());
    }

    @Path("/disconnected")
    @GET
    public Response getDisconnected(@Context final HttpServletRequest request) {
        List<SlackUser> disconnectedUsers = slackUserManager.findDisconnected();
        UserProfile currentUserProfile = userManager.getRemoteUser();
        String currentUserKey = currentUserProfile.getUserKey().getStringValue();
        Optional<String> disconnectedSlackUserId = disconnectedUsers.stream()
                .filter(user -> user.getUserKey().equals(currentUserKey))
                .map(SlackUser::getSlackUserId)
                .findFirst();

        List<SlackLink> disconnectedLinks = slackLinkManager.findDisconnected();
        boolean isGlobalAdmin = slackLinkAccessManager.hasAccess(currentUserProfile, request);
        boolean isAnyLinkDisconnected = !disconnectedLinks.isEmpty() && isGlobalAdmin;

        return Response.ok(new DisconnectedTokensDto(disconnectedSlackUserId.orElse(null), isAnyLinkDisconnected))
                .build();
    }

    @Path("dismiss-user-disconnection")
    @POST
    public Response dismissUserDisconnection(DismissUserDisconnectionDto data) {
        Optional<SlackUser> foundUser = slackUserManager.getBySlackUserId(data.getSlackUserId());
        foundUser.ifPresent(slackUserManager::delete);

        return Response.ok().build();
    }

    private static class Error {
        @JsonProperty
        private final String key;
        @JsonProperty
        private final String error;

        private Error(final String key, final String error) {
            this.key = key;
            this.error = error;
        }

        public String getError() {
            return error;
        }

        public String getKey() {
            return key;
        }
    }

    @RequiredArgsConstructor
    @JsonAutoDetect(getterVisibility = Visibility.PUBLIC_ONLY)
    public static class ConnectionStatusBean {
        private final ConnectionStatus status;

        public String getStatus() {
            return status.name();
        }
    }

    public enum ConnectionStatus {
        CONNECTED, PARTIALLY_CONNECTED
    }
}
