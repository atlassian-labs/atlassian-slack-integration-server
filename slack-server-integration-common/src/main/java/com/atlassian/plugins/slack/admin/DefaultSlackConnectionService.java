package com.atlassian.plugins.slack.admin;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugins.slack.analytics.AnalyticsContextProvider;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.SlackLinkDto;
import com.atlassian.plugins.slack.api.client.SlackClient;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.atlassian.plugins.slack.api.events.SlackWorkspaceRegisteredEvent;
import com.atlassian.plugins.slack.api.events.SlackWorkspaceUnregisteredEvent;
import com.atlassian.plugins.slack.api.events.SlackWorkspaceUpdatedEvent;
import com.atlassian.plugins.slack.event.SlackLinkedEvent;
import com.atlassian.plugins.slack.event.SlackTeamUnlinkedEvent;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.plugins.slack.oauth2.LinkErrorDataProvider;
import com.atlassian.plugins.slack.user.SlackUserManager;
import com.atlassian.plugins.slack.util.ErrorResponse;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.UrlMode;
import com.atlassian.sal.api.message.I18nResolver;
import com.github.seratch.jslack.api.methods.response.auth.AuthTestResponse;
import com.github.seratch.jslack.api.methods.response.oauth.OAuthAccessResponse;
import com.github.seratch.jslack.api.model.User;
import io.atlassian.fugue.Either;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Component
@Slf4j
public class DefaultSlackConnectionService implements SlackConnectionService {
    private final SlackLinkManager slackLinkManager;
    private final SlackUserManager slackUserManager;
    private final SlackClientProvider slackClientProvider;
    private final EventPublisher eventPublisher;
    private final I18nResolver i18nResolver;
    private final ApplicationProperties applicationProperties;
    private final AnalyticsContextProvider analyticsContextProvider;

    @Autowired
    public DefaultSlackConnectionService(
            final SlackLinkManager slackLinkManager,
            final SlackUserManager slackUserManager,
            final SlackClientProvider slackClientProvider,
            final EventPublisher eventPublisher,
            final I18nResolver i18nResolver,
            @Qualifier("salApplicationProperties") final ApplicationProperties applicationProperties,
            final AnalyticsContextProvider analyticsContextProvider) {
        this.slackLinkManager = slackLinkManager;
        this.slackUserManager = slackUserManager;
        this.slackClientProvider = slackClientProvider;
        this.eventPublisher = eventPublisher;
        this.i18nResolver = i18nResolver;
        this.applicationProperties = applicationProperties;
        this.analyticsContextProvider = analyticsContextProvider;
    }

    @Override
    public Either<ErrorResponse, InstallationCompletionData> connectTeam(final SlackLink slackLink, final String teamId) {
        try {
            // check if team is already connected
            final Either<Throwable, SlackLink> link = slackLinkManager.getLinkByTeamId(slackLink.getTeamId());
            if (isBlank(teamId) && link.isRight()) {
                return Either.left(new ErrorResponse(
                        new Exception(i18nResolver.getText("plugins.slack.admin.connect.workspace.already.connected")),
                        409));
            }

            // block team id change on link editing
            if (isNotBlank(teamId) && !teamId.equals(slackLink.getTeamId())) {
                return Either.left(new ErrorResponse(
                        new Exception(i18nResolver.getText("plugins.slack.admin.connect.workspace.error.team.id.changed")),
                        400));
            }

            return slackClientProvider.withLink(slackLink).testToken().flatMap(
                    resp -> {
                        if (!resp.getTeamId().equals(slackLink.getTeamId())) {
                            return Either.left(new ErrorResponse(new Exception(""), 401));
                        }

                        if (isNotBlank(teamId)) {
                            return slackLinkManager.saveExisting(slackLink).fold(
                                    e -> Either.left(new ErrorResponse(e)),
                                    updated -> Either.right(publishSuccessAndSendResult(updated, true)));
                        } else {
                            return slackLinkManager.saveNew(slackLink).fold(
                                    e -> Either.left(new ErrorResponse(e)),
                                    created -> Either.right(publishSuccessAndSendResult(created, false)));
                        }
                    }
            );
        } catch (Exception e) {
            return Either.left(new ErrorResponse(e));
        }
    }

    private InstallationCompletionData publishSuccessAndSendResult(final SlackLink slackLink,
                                                                   final boolean isUpdate) {
        if (isUpdate) {
            eventPublisher.publish(new SlackWorkspaceUpdatedEvent(analyticsContextProvider.bySlackLink(slackLink)));
        } else {
            eventPublisher.publish(new SlackLinkedEvent(slackLink));
            eventPublisher.publish(new SlackWorkspaceRegisteredEvent(analyticsContextProvider.bySlackLink(slackLink)));
        }
        return new InstallationCompletionData(slackLink.getTeamName(), slackLink.getTeamId());
    }

    @Override
    public Either<ErrorResponse, Boolean> disconnectTeam(final String teamId) {
        try {
            slackLinkManager.removeLinkByTeamId(teamId);
            slackUserManager.getByTeamId(teamId).forEach(slackUserManager::delete);
            eventPublisher.publish(new SlackTeamUnlinkedEvent(teamId));
            eventPublisher.publish(new SlackWorkspaceUnregisteredEvent(analyticsContextProvider.byTeamId(teamId)));
            return Either.right(true);
        } catch (Exception e) {
            return Either.left(new ErrorResponse(e));
        }
    }

    @Override
    public void enrichSlackLink(final SlackLinkDto dto) throws Exception {
        // enrich with services
        final Supplier<Exception> failedToObtainData = () -> new Exception(
                i18nResolver.getText("plugins.slack.admin.connect.workspace.error.invalid.credentials"));

        final SlackClient client = slackClientProvider.withLink(dto);

        // bot info
        final AuthTestResponse botAuth = client.testToken().getOrThrow(failedToObtainData);
        dto.setBotUserId(botAuth.getUserId());
        dto.setBotUserName(botAuth.getUser());
        dto.setTeamName(botAuth.getTeam());
        dto.setTeamId(botAuth.getTeamId());

        // installer info
        final AuthTestResponse installerAuth = client
                .withInstallerUserToken()
                .testToken()
                .getOrThrow(failedToObtainData);

        dto.setUserId(installerAuth.getUserId());

        // bot user info, which contains App ID
        final User botUser = client.getUserInfo(dto.getBotUserId()).getOrThrow(failedToObtainData);
        dto.setAppId(botUser.getProfile().getApiAppId());

        dto.setAppConfigurationUrl(null); // not used
        dto.setAppBlueprintId(null); //no blueprint id
        dto.setRawCredentials(null); //no raw credentials
    }

    @Override
    public Either<ErrorResponse, InstallationCompletionData> connectTeamViaOAuth(final String code) {
        // installing an App via OAuth 2 requires one initial connection with the App so we can borrow its credentials
        final List<SlackLink> allLinks = slackLinkManager.getLinks();
        final Optional<Pair<SlackLink, OAuthAccessResponse>> matchingLink = findMatchingLink(code, allLinks
                .stream()
                .limit(200)
                .collect(Collectors.toList()));

        // installation cannot have a state
        if (!matchingLink.isPresent()) {
            final String errorMessage = i18nResolver.getText("plugins.slack.admin.connect.workspace.oauth.install.no.matching.link");
            return Either.left(new ErrorResponse(new Exception(errorMessage), 401));
        }

        final SlackLink originalLink = matchingLink.get().getLeft();
        final OAuthAccessResponse response = matchingLink.get().getRight();

        final SlackLinkDto newLink = new SlackLinkDto();
        newLink.setClientId(originalLink.getClientId());
        newLink.setClientSecret(originalLink.getClientSecret());
        newLink.setSigningSecret(originalLink.getSigningSecret());
        newLink.setVerificationToken(originalLink.getVerificationToken());
        newLink.setAccessToken(response.getAccessToken());
        newLink.setBotAccessToken(response.getBot().getBotAccessToken());

        try {
            enrichSlackLink(newLink);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Either.left(new ErrorResponse(new Exception(e.getMessage()), 401));
        }

        return connectTeam(newLink, null);
    }

    private Optional<Pair<SlackLink, OAuthAccessResponse>> findMatchingLink(final String code,
                                                                            final List<SlackLink> links) {
        return links.parallelStream()
                .flatMap(link -> {
                    final Either<ErrorResponse, OAuthAccessResponse> response = slackClientProvider
                            .withLink(link)
                            .getOauthAccessToken(code, null);
                    return response
                            .map(value -> Stream.of(Pair.of(link, value)))
                            .getOr(Stream::empty);
                })
                .findAny();
    }

    @Override
    public URI redirectFromOAuth2Installation(final Either<ErrorResponse, InstallationCompletionData> result,
                                              final HttpServletRequest request) {
        // set error, if any
        result.left().forEach(e ->
                request.getSession().setAttribute(LinkErrorDataProvider.SLACK_OAUTH_ERROR_SESSION, e.getMessage()));

        // build base path
        final UriBuilder path = UriBuilder
                .fromPath(applicationProperties.getBaseUrl(UrlMode.ABSOLUTE))
                .path("/plugins/servlet/slack/configure");

        // add team parameter for new installation if not error
        result.forEach(data -> path.queryParam("recentInstall", data.getTeamId()));

        return path.build();
    }
}
