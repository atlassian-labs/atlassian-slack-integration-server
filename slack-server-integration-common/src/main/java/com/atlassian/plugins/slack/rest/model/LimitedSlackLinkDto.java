package com.atlassian.plugins.slack.rest.model;

import com.atlassian.plugins.slack.api.SlackLink;
import lombok.Value;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import static org.codehaus.jackson.annotate.JsonAutoDetect.Visibility.ANY;

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
