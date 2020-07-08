package com.atlassian.plugins.slack.ao;

import com.atlassian.plugins.slack.api.SlackLink;
import net.java.ao.Preload;
import net.java.ao.RawEntity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.PrimaryKey;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

@Table("AOSLACK_LINK")
@Preload
public interface AOSlackLink extends SlackLink, RawEntity<String> {
    @NotNull
    @PrimaryKey
    String getTeamId();

    void setTeamId(String teamId);

    void setClientId(String clientId);

    void setClientSecret(String clientSecret);

    @Indexed
    String getVerificationToken();

    void setVerificationToken(String verificationToken);

    void setAppId(String appId);

    void setAppBlueprintId(String appBlueprintId);

    void setUserId(String userId);

    void setTeamName(String teamName);

    void setAccessToken(String name);

    void setAppConfigurationUrl(String appConfigurationUrl);

    void setSigningSecret(String signingSecret);

    void setBotUserId(String botUserId);

    void setBotUserName(String botUserName);

    void setBotAccessToken(String name);

    @Indexed
    String getConnectionError();

    void setConnectionError(String connectionError);

    @StringLength(StringLength.UNLIMITED)
    String getRawCredentials();

    void setRawCredentials(String rawCredentials);
}
