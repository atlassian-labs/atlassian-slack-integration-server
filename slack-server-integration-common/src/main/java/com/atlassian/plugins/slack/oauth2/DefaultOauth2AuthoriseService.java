package com.atlassian.plugins.slack.oauth2;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugins.slack.analytics.AnalyticsContext;
import com.atlassian.plugins.slack.analytics.AnalyticsContextProvider;
import com.atlassian.plugins.slack.api.SlackUser;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.atlassian.plugins.slack.api.events.OauthFlowEvent;
import com.atlassian.plugins.slack.api.events.OauthFlowEvent.Status;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.plugins.slack.user.SlackUserManager;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.UrlMode;
import com.atlassian.sal.api.message.I18nResolver;
import com.atlassian.sal.api.user.UserKey;
import com.google.common.collect.ImmutableSet;
import io.atlassian.fugue.Either;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.core.UriBuilder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.net.URI;
import java.util.Set;

@Component
public class DefaultOauth2AuthoriseService implements Oauth2AuthoriseService {
    private final Set<String> CONFLUENCE_BITBUCKET_KEYS = ImmutableSet.of(
            ApplicationProperties.PLATFORM_CONFLUENCE,
            ApplicationProperties.PLATFORM_BITBUCKET,
            ApplicationProperties.PLATFORM_STASH
    );

    private final SlackLinkManager slackLinkManager;
    private final SlackUserManager slackUserManager;
    private final SlackClientProvider slackClientProvider;
    private final ApplicationProperties applicationProperties;
    private final I18nResolver i18nResolver;
    private final EventPublisher eventPublisher;
    private final AnalyticsContextProvider analyticsContextProvider;

    @Autowired
    public DefaultOauth2AuthoriseService(
            final SlackLinkManager slackLinkManager,
            final SlackUserManager slackUserManager,
            final SlackClientProvider slackClientProvider,
            @Qualifier("salApplicationProperties") final ApplicationProperties applicationProperties,
            final I18nResolver i18nResolver,
            final EventPublisher eventPublisher,
            final AnalyticsContextProvider analyticsContextProvider) {
        this.slackLinkManager = slackLinkManager;
        this.slackUserManager = slackUserManager;
        this.slackClientProvider = slackClientProvider;
        this.applicationProperties = applicationProperties;
        this.i18nResolver = i18nResolver;
        this.eventPublisher = eventPublisher;
        this.analyticsContextProvider = analyticsContextProvider;
    }

    private boolean isConfluenceOrBitbucket() {
        String platformId = applicationProperties.getPlatformId();
        return CONFLUENCE_BITBUCKET_KEYS.contains(platformId);
    }

    private String getRedirectUri(final String teamId) {
        return UriBuilder.fromUri(applicationProperties.getBaseUrl(UrlMode.CANONICAL))
                .path(isConfluenceOrBitbucket() ? "rest/slack/latest" : "slack")
                .path("oauth")
                .path("redirect")
                .path(teamId)
                .build()
                .toString();
    }

    @Override
    public Either<Throwable, URI> beginOauth2(final Oauth2BeginData data) {
        // https://hello.atlassian.net/wiki/spaces/PI/pages/459027359/Bitbucket+Server+for+Slack+-+App+permissions
        final String scopes = isConfluenceOrBitbucket()
                ? "channels:read channels:write groups:read groups:write links:read links:write"
                : "channels:read channels:write groups:read groups:write chat:write:bot links:read links:write";
        return slackLinkManager.getLinkByTeamId(data.getTeamId()).fold(
                Either::left,
                link -> {
                    final URI slackOauthUri = UriBuilder.fromUri("https://slack.com/oauth/authorize")
                            .queryParam("client_id", link.getClientId())
                            .queryParam("scope", scopes)
                            .queryParam("redirect_uri", getRedirectUri(data.getTeamId()))
                            .queryParam("state", data.getSecret())
                            .queryParam("team", link.getTeamId())
                            .build();

                    final UriBuilder originUriBuilder = UriBuilder.fromUri(applicationProperties.getBaseUrl(UrlMode.ABSOLUTE))
                            .path(data.getRedirect())
                            .replaceQuery(data.getRedirectQuery());
                    if (StringUtils.isNotBlank(data.getFragment())) {
                        originUriBuilder.fragment(data.getFragment());
                    }

                    final HttpSession session = data.getServletRequest().getSession();
                    final String originUrl = originUriBuilder.build().toString();
                    session.setAttribute(data.getSecret() + "-origin-url", originUrl);

                    eventPublisher.publish(new OauthFlowEvent(analyticsContextProvider.bySlackLink(link), Status.STARTED));

                    return Either.right(slackOauthUri);
                }
        );
    }

    @Override
    public Either<Throwable, URI> completeOauth2(final Oauth2CompleteData data) {
        if (StringUtils.isBlank(data.getState())) {
            return Either.left(new Exception("invalid state"));
        }

        final URI redirectUri = getOriginUrl(data.getState(), data.getRequest());
        return slackClientProvider.withTeamId(data.getTeamId()).fold(
                Either::left,
                client -> client.getOauthAccessToken(data.getCode(), getRedirectUri(data.getTeamId())).fold(
                        e -> Either.left(e.getException()),
                        response -> {
                            if (!data.getTeamId().equals(response.getTeamId())) {
                                return Either.left(new Exception(i18nResolver.getText("slack.oauth2.error.message.invalid.team")));
                            }

                            final SlackUser slackUser = slackUserManager.create(
                                    response.getUserId(),
                                    data.getUserKey(),
                                    client.getLink());
                            slackUserManager.updatePersonalToken(slackUser.getSlackUserId(), response.getAccessToken());

                            eventPublisher.publish(new OauthFlowEvent(AnalyticsContext.fromSlackUser(slackUser), Status.SUCCEEDED));

                            return Either.right(redirectUri);
                        }
                )
        );
    }

    @Override
    public URI rejectedOAuth2(final String teamId,
                              final String state,
                              final String error,
                              final HttpServletRequest request) {
        request.getSession().setAttribute(LinkErrorDataProvider.SLACK_OAUTH_ERROR_SESSION, error);
        eventPublisher.publish(new OauthFlowEvent(analyticsContextProvider.byTeamId(teamId), Status.FAILED));
        return getOriginUrl(state, request);
    }

    private URI getOriginUrl(final String state, final HttpServletRequest request) {
        final HttpSession session = request.getSession();
        final String originUri = (String) session.getAttribute(state + "-origin-url");
        return URI.create(StringUtils.defaultString(originUri, applicationProperties.getBaseUrl(UrlMode.ABSOLUTE)));
    }

    @Override
    public boolean removeOauth2Configuration(@Nonnull final UserKey userKey, final String teamId) {
        return slackUserManager.getByTeamIdAndUserKey(teamId, userKey.getStringValue())
                .map(u -> {
                    slackUserManager.delete(u);
                    return u;
                })
                .isPresent();
    }
}
