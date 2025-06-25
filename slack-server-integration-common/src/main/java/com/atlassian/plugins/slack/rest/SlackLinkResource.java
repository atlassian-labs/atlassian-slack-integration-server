package com.atlassian.plugins.slack.rest;

import com.atlassian.confluence.compat.api.service.accessmode.ReadOnlyAccessAllowed;
import com.atlassian.plugins.slack.admin.SlackConnectionService;
import com.atlassian.plugins.slack.api.SlackLinkDto;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.plugins.slack.rest.model.GetWorkspacesResponse;
import com.atlassian.plugins.slack.rest.model.LimitedSlackLinkDto;
import com.atlassian.sal.api.message.I18nResolver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.stream.Collectors;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;

@ReadOnlyAccessAllowed
@Path("/connection")
public class SlackLinkResource {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final SlackConnectionService slackConnectionService;
    private final SlackLinkManager slackLinkManager;
    private final I18nResolver i18nResolver;
    private final SlackClientProvider slackClientProvider;

    @Inject
    @Autowired
    public SlackLinkResource(final SlackConnectionService slackConnectionService,
                             final SlackLinkManager slackLinkManager,
                             final I18nResolver i18nResolver,
                             final SlackClientProvider slackClientProvider) {
        this.slackConnectionService = slackConnectionService;
        this.slackLinkManager = slackLinkManager;
        this.i18nResolver = i18nResolver;
        this.slackClientProvider = slackClientProvider;
    }

    private String nonNullString(final JsonNode node) {
        String textValue = node.asText();
        if (textValue == null || textValue.isEmpty()) {
            throw new IllegalArgumentException("Required field has no value");
        }
        return textValue;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getLinkedTeams() {
        return Response.ok(new GetWorkspacesResponse(
                slackLinkManager.getLinks().stream()
                        .map(LimitedSlackLinkDto::new)
                        .collect(Collectors.toList())
        )).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @SlackLinkAdministerPermission
    public Response createTeam(final String body) {
        return updateTeam(null, body);
    }

    @POST
    @Path("/{teamId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @SlackLinkAdministerPermission
    public Response updateTeam(@PathParam("teamId") final String teamId, final String body) {
        final SlackLinkDto dto = new SlackLinkDto();
        boolean isCustom = false;
        try {
            final JsonNode credentialsNode = OBJECT_MAPPER.readTree(body);
            isCustom = credentialsNode.path("custom").asBoolean(false);

            dto.setClientId(nonNullString(credentialsNode.path("client_id")));
            dto.setClientSecret(nonNullString(credentialsNode.path("client_secret")));
            dto.setSigningSecret(nonNullString(credentialsNode.path("signing_secret")));
            dto.setVerificationToken(nonNullString(credentialsNode.path("verification_token")));
            dto.setAccessToken(nonNullString(credentialsNode.path("access_token")));
            dto.setBotAccessToken(nonNullString(credentialsNode.path("bot_access_token")));

            if (isCustom) {
                slackConnectionService.enrichSlackLink(dto);
            } else {
                dto.setAppId(nonNullString(credentialsNode.path("app_id")));
                dto.setAppBlueprintId(nonNullString(credentialsNode.path("app_blueprint_id")));
                dto.setUserId(nonNullString(credentialsNode.path("user_id")));
                dto.setTeamName(nonNullString(credentialsNode.path("team_name")));
                dto.setTeamId(nonNullString(credentialsNode.path("team_id")));
                dto.setAppConfigurationUrl(nonNullString(credentialsNode.path("app_configuration_url")));
                dto.setBotUserId(nonNullString(credentialsNode.path("bot_user_id")));
                dto.setBotUserName(nonNullString(credentialsNode.path("bot_username")));
                dto.setRawCredentials(body);
            }
        } catch (Throwable e) {
            String errorMessageKey = isCustom
                    ? "plugins.slack.admin.connect.workspace.error.invalid.format.advanced"
                    : "plugins.slack.admin.connect.workspace.error.invalid.format.basic";
            return Response
                    .status(BAD_REQUEST)
                    .entity(i18nResolver.getText(errorMessageKey))
                    .build();
        }

        return slackConnectionService.connectTeam(dto, teamId).fold(
                error -> {
                    if (error.getStatusCode() == 401) {
                        return Response
                                .status(BAD_REQUEST)
                                .entity(i18nResolver.getText("plugins.slack.admin.connect.workspace.error.not.accepted"))
                                .build();
                    } else if (error.getStatusCode() == 409) {
                        return Response
                                .status(BAD_REQUEST)
                                .entity(i18nResolver.getText("plugins.slack.admin.connect.workspace.already.connected"))
                                .build();
                    } else {
                        return Response
                                .status(BAD_REQUEST)
                                .entity(StringUtils.defaultIfBlank(error.getMessage(),
                                        i18nResolver.getText("plugins.slack.admin.connect.workspace.error.generic")))
                                .build();

                    }
                },
                data -> Response.ok(data).build());
    }

    @DELETE
    @Path("/{teamId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @SlackLinkAdministerPermission
    public Response deleteTeam(@PathParam("teamId") final String teamId) {
        return slackConnectionService.disconnectTeam(teamId).fold(
                e -> Response.status(BAD_REQUEST).build(),
                ignored -> Response.noContent().build());
    }

    @POST
    @Path("/can-reach-slack")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @SlackLinkAdministerPermission
    public Response validateSlackReachability() {
        return slackClientProvider.withoutCredentials().testApi().fold(
                e -> Response
                        .serverError()
                        .status(BAD_REQUEST)
                        .type(MediaType.TEXT_PLAIN_TYPE)
                        .entity("Failed to ping Slack. See logs for more information.")
                        .build(),
                r -> Response.ok().build()
        );
    }

    @GET
    @Path("/{teamId}/info")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getTeamInfo(@PathParam("teamId") final String teamId) {
        return slackClientProvider.withTeamId(teamId).fold(
                e -> Response.status(NOT_FOUND).build(),
                client -> client.testToken().fold(
                        e -> Response.status(NOT_FOUND).build(),
                        resp -> Response.ok(
                                ImmutableMap.<String, Object>builder()
                                        .put("teamUrl", resp.getUrl())
                                        .build())
                                .build()
                )
        );
    }
}
