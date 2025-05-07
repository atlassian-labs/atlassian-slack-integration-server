package com.atlassian.plugins.slack.rest;

import com.atlassian.confluence.compat.api.service.accessmode.ReadOnlyAccessAllowed;
import com.atlassian.plugins.slack.admin.InstallationCompletionData;
import com.atlassian.plugins.slack.admin.SlackConnectionService;
import com.atlassian.plugins.slack.admin.XsrfTokenGenerator;
import com.atlassian.plugins.slack.admin.XsrfTokenGenerator.ValidationResult;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.plugins.slack.oauth2.Oauth2AuthoriseService;
import com.atlassian.plugins.slack.oauth2.Oauth2BeginData;
import com.atlassian.plugins.slack.oauth2.Oauth2CompleteData;
import com.atlassian.plugins.slack.rest.model.OauthRequestData;
import com.atlassian.plugins.slack.util.ErrorResponse;
import com.atlassian.sal.api.message.I18nResolver;
import com.atlassian.sal.api.user.UserKey;
import com.atlassian.sal.api.user.UserManager;
import io.atlassian.fugue.Either;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@ReadOnlyAccessAllowed
@Path("/oauth")
public class SlackOAuth2Resource {
    private final Oauth2AuthoriseService oauth2AuthoriseService;
    private final XsrfTokenGenerator tokenGenerator;
    private final SlackLinkManager slackLinkManager;
    private final UserManager userManager;
    private final I18nResolver i18nResolver;
    private final SlackConnectionService slackConnectionService;

    @Inject
    public SlackOAuth2Resource(final Oauth2AuthoriseService oauth2AuthoriseService,
                               final XsrfTokenGenerator tokenGenerator,
                               final SlackLinkManager slackLinkManager,
                               @Named("salUserManager") final UserManager userManager,
                               final I18nResolver i18nResolver,
                               final SlackConnectionService slackConnectionService) {
        this.oauth2AuthoriseService = oauth2AuthoriseService;
        this.tokenGenerator = tokenGenerator;
        this.slackLinkManager = slackLinkManager;
        this.userManager = userManager;
        this.i18nResolver = i18nResolver;
        this.slackConnectionService = slackConnectionService;
    }

    @GET
    @SlackLinkAdministerPermission
    public Response installViaOauth2(@Context final HttpServletRequest servletRequest,
                                     @Context final HttpServletResponse servletResponse,
                                     @QueryParam("code") final String code,
                                     @QueryParam("state") final String state) {
        // installation cannot have a state
        if (isNotBlank(state)) {
            final ErrorResponse errorResponse = new ErrorResponse(
                    new Exception(i18nResolver.getText("plugins.slack.admin.connect.workspace.oauth.install.invalid")));
            return Response
                    .temporaryRedirect(slackConnectionService.redirectFromOAuth2Installation(Either.left(errorResponse), servletRequest))
                    .build();
        }

        final Either<ErrorResponse, InstallationCompletionData> result = slackConnectionService.connectTeamViaOAuth(code);
        return Response
                .temporaryRedirect(slackConnectionService.redirectFromOAuth2Installation(result, servletRequest))
                .build();
    }

    @POST
    @Path("/begin/{teamId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response beginOauth2(@Context final HttpServletRequest servletRequest,
                                @Context final HttpServletResponse servletResponse,
                                @PathParam("teamId") final String teamId,
                                final OauthRequestData oauthRequestData) {
        Either<Throwable, URI> oauthUrl = oauth2AuthoriseService
                .beginOauth2(
                        new Oauth2BeginData(
                                servletRequest,
                                oauthRequestData.getRedirect(),
                                oauthRequestData.getRedirectQuery(),
                                oauthRequestData.getRedirectFragment(),
                                userManager.getRemoteUserKey(),
                                teamId,
                                tokenGenerator.getNewToken(servletRequest, teamId)));
        return oauthUrl.fold(
                e -> Response.serverError().entity(e).build(),
                data -> Response.ok(data.toASCIIString()).build());

    }

    @GET
    @Path("/redirect/{teamId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response completeOauth2(@Context final HttpServletRequest servletRequest,
                                   @Context final HttpServletResponse servletResponse,
                                   @PathParam("teamId") final String teamId,
                                   @QueryParam("error") final String errorParam,
                                   @QueryParam("code") final String code,
                                   @QueryParam("state") final String state) {
        Either<Throwable, SlackLink> linkOptional = slackLinkManager.getLinkByTeamId(teamId);
        if (!linkOptional.isRight()) {
            return Response
                    .temporaryRedirect(oauth2AuthoriseService.rejectedOAuth2(
                            teamId, state,
                            i18nResolver.getText("slack.oauth2.error.link.not.found", teamId),
                            servletRequest))
                    .build();
        }

        if (StringUtils.isNotBlank(errorParam)) {
            final String errMessage = "access_denied".equals(errorParam)
                    ? i18nResolver.getText("slack.oauth2.error.message.access.denied")
                    : i18nResolver.getText("slack.oauth2.error.message", errorParam);
            return Response
                    .temporaryRedirect(oauth2AuthoriseService.rejectedOAuth2(teamId, state, errMessage, servletRequest))
                    .build();
        }

        final ValidationResult validationResult = tokenGenerator.validateToken(servletRequest, state, teamId);
        if (validationResult.equals(ValidationResult.UNKNOWN)) {
            return Response
                    .temporaryRedirect(oauth2AuthoriseService.rejectedOAuth2(
                            teamId, state,
                            i18nResolver.getText("slack.oauth2.error.message.invalid.token"),
                            servletRequest))
                    .build();
        }

        servletRequest.getSession().removeAttribute(state);
        if (validationResult.equals(ValidationResult.INVALID_TEAM)) {
            return Response
                    .temporaryRedirect(oauth2AuthoriseService.rejectedOAuth2(
                            teamId, state,
                            i18nResolver.getText("slack.oauth2.error.message.invalid.team"),
                            servletRequest))
                    .build();
        }

        final UserKey userKey = userManager.getRemoteUserKey();
        final Oauth2CompleteData data = new Oauth2CompleteData(code, servletRequest, userKey, teamId, state);

        return oauth2AuthoriseService.completeOauth2(data).fold(
                e -> Response
                        .temporaryRedirect(oauth2AuthoriseService.rejectedOAuth2(
                                teamId, state,
                                i18nResolver.getText("slack.oauth2.error.message", e.getMessage()),
                                servletRequest))
                        .build(),
                uri -> Response.temporaryRedirect(uri).build());
    }

    @DELETE
    @Path("/{teamId}")
    public Response deleteUserLink(@PathParam("teamId") final String teamId) {
        final UserKey userKey = userManager.getRemoteUserKey();
        if (userKey == null) {
            return Response
                    .status(Response.Status.FORBIDDEN)
                    .build();
        }
        if (!oauth2AuthoriseService.removeOauth2Configuration(userKey, teamId)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }

}
