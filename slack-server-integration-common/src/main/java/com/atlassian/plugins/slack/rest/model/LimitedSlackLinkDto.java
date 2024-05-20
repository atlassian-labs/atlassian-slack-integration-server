package com.atlassian.plugins.slack.rest.model;

import com.atlassian.plugins.slack.api.SlackLink;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Value;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;

@SuppressWarnings("unused")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = ANY)
@Value
public final class LimitedSlackLinkDto {
    String teamName;
    String teamId;
    String appConfigurationUrl;
    String botUserId;
    String botUserName;
    String connectionError;

    public LimitedSlackLinkDto(final SlackLink link) {
        this.teamName = link.getTeamName();
        this.teamId = link.getTeamId();
        this.appConfigurationUrl = link.getAppConfigurationUrl();
        this.botUserId = link.getBotUserId();
        this.botUserName = link.getBotUserName();
        this.connectionError = link.getConnectionError();
    }
}
