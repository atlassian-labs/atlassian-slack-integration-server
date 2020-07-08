package com.atlassian.plugins.slack.api;

public interface SlackLink {
    String getConnectionError();

    String getClientId();

    String getClientSecret();

    String getVerificationToken();

    String getAppId();

    String getAppBlueprintId();

    String getUserId();

    String getTeamName();

    String getTeamId();

    String getAccessToken();

    String getAppConfigurationUrl();

    String getSigningSecret();

    String getBotUserId();

    String getBotUserName();

    String getBotAccessToken();

    String getRawCredentials();
}
