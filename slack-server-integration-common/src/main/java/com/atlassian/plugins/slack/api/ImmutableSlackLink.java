package com.atlassian.plugins.slack.api;

import java.io.Serializable;

public final class ImmutableSlackLink implements SlackLink, Serializable {
    private final String clientId;
    private final String clientSecret;
    private final String verificationToken;
    private final String appId;
    private final String appBlueprintId;
    private final String userId;
    private final String accessToken;
    private final String teamName;
    private final String teamId;
    private final String appConfigurationUrl;
    private final String signingSecret;
    private final String botUserId;
    private final String botUserName;
    private final String botAccessToken;
    private final String rawCredentials;
    private final String connectionError;

    public ImmutableSlackLink(final SlackLink slackLink) {
        this(
                slackLink.getClientId(),
                slackLink.getClientSecret(),
                slackLink.getVerificationToken(),
                slackLink.getAppId(),
                slackLink.getAppBlueprintId(),
                slackLink.getUserId(),
                slackLink.getAccessToken(),
                slackLink.getTeamName(),
                slackLink.getTeamId(),
                slackLink.getAppConfigurationUrl(),
                slackLink.getSigningSecret(),
                slackLink.getBotUserId(),
                slackLink.getBotUserName(),
                slackLink.getBotAccessToken(),
                slackLink.getRawCredentials(),
                slackLink.getConnectionError());
    }

    public ImmutableSlackLink(final String clientId,
                              final String clientSecret,
                              final String verificationToken,
                              final String appId,
                              final String appBlueprintId,
                              final String userId,
                              final String accessToken,
                              final String teamName,
                              final String teamId,
                              final String appConfigurationUrl,
                              final String signingSecret,
                              final String botUserId,
                              final String botUserName,
                              final String botAccessToken,
                              final String rawCredentials,
                              final String connectionError) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.verificationToken = verificationToken;
        this.appId = appId;
        this.appBlueprintId = appBlueprintId;
        this.userId = userId;
        this.accessToken = accessToken;
        this.teamName = teamName;
        this.teamId = teamId;
        this.appConfigurationUrl = appConfigurationUrl;
        this.signingSecret = signingSecret;
        this.botUserId = botUserId;
        this.botUserName = botUserName;
        this.botAccessToken = botAccessToken;
        this.rawCredentials = rawCredentials;
        this.connectionError = connectionError;
    }

    @Override
    public String getClientId() {
        return clientId;
    }

    @Override
    public String getClientSecret() {
        return clientSecret;
    }

    @Override
    public String getVerificationToken() {
        return verificationToken;
    }

    @Override
    public String getAppId() {
        return appId;
    }

    @Override
    public String getAppBlueprintId() {
        return appBlueprintId;
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public String getTeamName() {
        return teamName;
    }

    @Override
    public String getTeamId() {
        return teamId;
    }

    @Override
    public String getAccessToken() {
        return accessToken;
    }

    @Override
    public String getAppConfigurationUrl() {
        return appConfigurationUrl;
    }

    @Override
    public String getSigningSecret() {
        return signingSecret;
    }

    @Override
    public String getBotUserId() {
        return botUserId;
    }

    @Override
    public String getBotUserName() {
        return botUserName;
    }

    @Override
    public String getBotAccessToken() {
        return botAccessToken;
    }

    @Override
    public String getRawCredentials() {
        return rawCredentials;
    }

    @Override
    public String getConnectionError() {
        return connectionError;
    }
}
