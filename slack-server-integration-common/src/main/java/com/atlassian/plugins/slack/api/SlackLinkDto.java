package com.atlassian.plugins.slack.api;

import java.io.Serializable;

public final class SlackLinkDto implements SlackLink, Serializable {
    private String clientId;
    private String clientSecret;
    private String verificationToken;
    private String appId;
    private String appBlueprintId;
    private String userId;
    private String accessToken;
    private String teamName;
    private String teamId;
    private String appConfigurationUrl;
    private String signingSecret;
    private String botUserId;
    private String botUserName;
    private String botAccessToken;
    private String rawCredentials;
    private String connectionError;

    public SlackLinkDto() {
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

    public void setClientId(final String clientId) {
        this.clientId = clientId;
    }

    public void setClientSecret(final String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public void setVerificationToken(final String verificationToken) {
        this.verificationToken = verificationToken;
    }

    public void setAppId(final String appId) {
        this.appId = appId;
    }

    public void setAppBlueprintId(final String appBlueprintId) {
        this.appBlueprintId = appBlueprintId;
    }

    public void setUserId(final String userId) {
        this.userId = userId;
    }

    public void setAccessToken(final String accessToken) {
        this.accessToken = accessToken;
    }

    public void setTeamName(final String teamName) {
        this.teamName = teamName;
    }

    public void setTeamId(final String teamId) {
        this.teamId = teamId;
    }

    public void setAppConfigurationUrl(final String appConfigurationUrl) {
        this.appConfigurationUrl = appConfigurationUrl;
    }

    public void setSigningSecret(final String signingSecret) {
        this.signingSecret = signingSecret;
    }

    public void setBotUserId(final String botUserId) {
        this.botUserId = botUserId;
    }

    public void setBotUserName(final String botUserName) {
        this.botUserName = botUserName;
    }

    public void setBotAccessToken(final String botAccessToken) {
        this.botAccessToken = botAccessToken;
    }

    public void setRawCredentials(final String rawCredentials) {
        this.rawCredentials = rawCredentials;
    }

    public void setConnectionError(final String connectionError) {
        this.connectionError = connectionError;
    }
}
