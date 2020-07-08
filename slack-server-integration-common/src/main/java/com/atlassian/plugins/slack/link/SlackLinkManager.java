package com.atlassian.plugins.slack.link;

import com.atlassian.plugins.slack.api.SlackLink;
import io.atlassian.fugue.Either;

import java.util.List;
import java.util.Optional;

public interface SlackLinkManager {
    boolean shouldUseLinkUnfurl(String teamId);

    boolean isAnyLinkDefined();

    Optional<SlackLink> isAnyLinkDisconnected();

    List<SlackLink> findDisconnected();

    List<SlackLink> getLinks();

    Either<Throwable, SlackLink> getLinkByVerificationToken(String verificationToken);

    Either<Throwable, SlackLink> getLinkBySigningSecret(final String signingSecret);

    Either<Throwable, SlackLink> getLinkByTeamId(String teamId);

    void removeLinkByTeamId(String teamId);

    Either<Throwable, SlackLink> saveExisting(SlackLink slackLink);

    Either<Throwable, SlackLink> setConnectionError(String teamId, String error);

    Either<Throwable, SlackLink> saveNew(SlackLink slackLink);

    void revokeToken(String teamId);
}
