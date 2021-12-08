package com.atlassian.plugins.slack.api.webhooks;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;
import java.util.Map;

import static org.codehaus.jackson.annotate.JsonAutoDetect.Visibility.ANY;

/*
https://api.slack.com/events/tokens_revoked
{
    "api_app_id": "A00T0R11P66",
    "event": {
        "event_ts": "1633245770.103223",
        "tokens": {
            "bot": [
                "U77KMNSACCM"
            ],
            "oauth": [
                "U22JHNBLDNF"
            ]
        },
        "type": "tokens_revoked"
    },
    "event_id": "Es36PQNZWHSP",
    "event_time": 1238349970,
    "team_id": "TT0EEPP4R",
    "token": "dr2FDH3al54fFGWEJHsNdo0u",
    "type": "event_callback"
}
*/
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = ANY)
public class TokensRevokedSlackEvent implements SlackEventHolder {
    public static final String TYPE = "tokens_revoked";

    private SlackEvent slackEvent;
    private List<String> userIds;
    private List<String> botIds;

    @JsonProperty("tokens")
    private void unpackTokens(Map<String, Object> tokens) {
        setUserIds((List<String>) tokens.get("oauth"));
        setBotIds((List<String>) tokens.get("bot"));
    }

    public SlackEvent getSlackEvent() {
        return slackEvent;
    }

    public void setSlackEvent(final SlackEvent slackEvent) {
        this.slackEvent = slackEvent;
    }

    public List<String> getUserIds() {
        return userIds;
    }

    public void setUserIds(final List<String> userIds) {
        this.userIds = userIds;
    }

    public List<String> getBotIds() {
        return botIds;
    }

    public void setBotIds(final List<String> botIds) {
        this.botIds = botIds;
    }
}
