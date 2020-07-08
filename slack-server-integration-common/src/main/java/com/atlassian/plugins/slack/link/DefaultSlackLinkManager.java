package com.atlassian.plugins.slack.link;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugins.slack.ao.AOSlackLink;
import com.atlassian.plugins.slack.api.ImmutableSlackLink;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.TeamNotConnectedException;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import io.atlassian.fugue.Either;
import net.java.ao.DBParam;
import net.java.ao.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.ArrayUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Component
public class DefaultSlackLinkManager implements SlackLinkManager {
    private static final String DISCONNECTED_CLAUSE = "CONNECTION_ERROR IS NOT NULL";

    private final ActiveObjects ao;
    private final TransactionTemplate transactionTemplate;

    @Autowired
    public DefaultSlackLinkManager(final ActiveObjects ao,
                                   final TransactionTemplate transactionTemplate) {
        this.ao = ao;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public void removeLinkByTeamId(final String teamId) {
        transactionTemplate.execute(() -> {
            getAoById(teamId).ifPresent(ao::delete);
            return null;
        });
    }

    private Either<Throwable, SlackLink> getBy(final String clause, final String value) {
        try {
            final AOSlackLink[] results = ao.find(
                    AOSlackLink.class,
                    Query.select().where(clause, value));

            if (results == null || results.length == 0) {
                return Either.left(new TeamNotConnectedException(value));
            }

            return Either.right(toImmutable(results[0]));
        } catch (Throwable e) {
            return Either.left(e);
        }
    }

    @Override
    public Either<Throwable, SlackLink> getLinkByVerificationToken(final String verificationToken) {
        return getBy("VERIFICATION_TOKEN = ?", verificationToken);
    }

    @Override
    public Either<Throwable, SlackLink> getLinkBySigningSecret(final String signingSecret) {
        return getBy("SIGNING_SECRET = ?", signingSecret);
    }

    @Override
    public Either<Throwable, SlackLink> getLinkByTeamId(final String teamId) {
        return getBy("TEAM_ID = ?", teamId);
    }

    @Override
    public boolean shouldUseLinkUnfurl(final String teamId) {
        // system variable overrides behavior
        final String unfurlEnabled = System.getProperty("slack.unfurl.enabled", System.getProperty("jira.slack.unfurl.enabled"));
        if (unfurlEnabled != null) {
            return Boolean.valueOf(unfurlEnabled);
        }
        return getLinkByTeamId(teamId)
                .toOptional()
                .map(link -> isBlank(link.getRawCredentials()))
                .orElse(false);
    }

    @Override
    public boolean isAnyLinkDefined() {
        return ao.count(AOSlackLink.class, Query.select()) > 0;
    }

    @Override
    public Optional<SlackLink> isAnyLinkDisconnected() {
        return getBy("CONNECTION_ERROR IS NOT NULL AND CONNECTION_ERROR != ?", "").fold(e -> Optional.empty(), Optional::of);
    }

    @Override
    public List<SlackLink> findDisconnected() {
        AOSlackLink[] links = ao.find(AOSlackLink.class, Query.select().where(DISCONNECTED_CLAUSE));

        if (isEmpty(links)) {
            return Collections.emptyList();
        }

        return toList(links);
    }

    @Override
    public List<SlackLink> getLinks() {
        final AOSlackLink[] results = ao.find(AOSlackLink.class);
        if (isEmpty(results)) {
            return Collections.emptyList();
        }
        return toList(results);
    }

    @Override
    public Either<Throwable, SlackLink> saveExisting(final SlackLink link) {
        try {
            return transactionTemplate.execute(() -> {
                final Optional<AOSlackLink> savedLink = getAoById(link.getTeamId()).map(existing -> {
                    copy(link, existing);
                    existing.save();
                    return existing;
                });

                return savedLink
                        .map(slackLink -> Either.<Throwable, SlackLink>right(toImmutable(slackLink)))
                        .orElseGet(() -> Either.left(new Exception("Could not find link for team " + link.getTeamId())));
            });
        } catch (Exception e) {
            return Either.left(e);
        }
    }

    @Override
    public Either<Throwable, SlackLink> setConnectionError(final String teamId, final String error) {
        try {
            return transactionTemplate.execute(() -> {
                final Optional<AOSlackLink> link = getAoById(teamId).map(existing -> {
                    existing.setConnectionError(error);
                    existing.save();
                    return existing;
                });

                return link
                        .map(slackLink -> Either.<Throwable, SlackLink>right(toImmutable(slackLink)))
                        .orElseGet(() -> Either.left(new Exception("Could not find link for team " + teamId)));
            });
        } catch (Exception e) {
            return Either.left(e);
        }
    }

    @Override
    public Either<Throwable, SlackLink> saveNew(final SlackLink link) {
        try {
            return transactionTemplate.execute(() -> {
                final AOSlackLink newLink = ao.create(AOSlackLink.class,
                        new DBParam("APP_BLUEPRINT_ID", link.getAppBlueprintId()),
                        new DBParam("APP_CONFIGURATION_URL", link.getAppConfigurationUrl()),
                        new DBParam("APP_ID", link.getAppId()),
                        new DBParam("BOT_ACCESS_TOKEN", link.getBotAccessToken()),
                        new DBParam("USER_ID", link.getUserId()),
                        new DBParam("BOT_USER_ID", link.getBotUserId()),
                        new DBParam("BOT_USER_NAME", link.getBotUserName()),
                        new DBParam("CLIENT_ID", link.getClientId()),
                        new DBParam("CLIENT_SECRET", link.getClientSecret()),
                        new DBParam("RAW_CREDENTIALS", link.getRawCredentials()),
                        new DBParam("SIGNING_SECRET", link.getSigningSecret()),
                        new DBParam("TEAM_ID", link.getTeamId()),
                        new DBParam("TEAM_NAME", link.getTeamName()),
                        new DBParam("VERIFICATION_TOKEN", link.getVerificationToken()),
                        new DBParam("ACCESS_TOKEN", link.getAccessToken()),
                        new DBParam("CONNECTION_ERROR", link.getConnectionError())
                );

                return Either.right(toImmutable(newLink));
            });
        } catch (Exception e) {
            return Either.left(e);
        }
    }

    @Override
    public void revokeToken(final String teamId) {
        transactionTemplate.execute(() -> {
            getAoById(teamId).ifPresent(link -> {
                link.setBotAccessToken(null);
                link.setAccessToken(null);
                link.setConnectionError("Token was revoked");
                link.save();
            });
            return null;
        });
    }

    private Optional<AOSlackLink> getAoById(final String teamId) {
        return Optional.ofNullable(ao.get(AOSlackLink.class, teamId));
    }

    private void copy(final SlackLink from, final AOSlackLink to) {
        to.setAppBlueprintId(from.getAppBlueprintId());
        to.setAppConfigurationUrl(from.getAppConfigurationUrl());
        to.setAppId(from.getAppId());
        to.setBotAccessToken(from.getBotAccessToken());
        to.setUserId(from.getUserId());
        to.setBotUserId(from.getBotUserId());
        to.setBotUserName(from.getBotUserName());
        to.setClientId(from.getClientId());
        to.setClientSecret(from.getClientSecret());
        to.setRawCredentials(from.getRawCredentials());
        to.setSigningSecret(from.getSigningSecret());
        to.setTeamId(from.getTeamId());
        to.setTeamName(from.getTeamName());
        to.setVerificationToken(from.getVerificationToken());
        to.setAccessToken(from.getAccessToken());
        to.setConnectionError(from.getConnectionError());
    }

    private List<SlackLink> toList(final AOSlackLink[] results) {
        return Arrays.stream(results)
                .map(ImmutableSlackLink::new)
                .collect(Collectors.toList());
    }

    private SlackLink toImmutable(final AOSlackLink result) {
        return new ImmutableSlackLink(result);
    }
}
