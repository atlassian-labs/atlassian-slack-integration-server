package com.atlassian.plugins.slack.api.webhooks;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import static org.codehaus.jackson.annotate.JsonAutoDetect.Visibility.ANY;

/*
https://api.slack.com/events/app_uninstalled
{
    "token": "XXYYZZ",
    "team_id": "TXXXXXXXX",
    "api_app_id": "AXXXXXXXXX",
    "event": {
        "type": "app_uninstalled"
    },
    "type": "event_callback",
    "event_id": "EvXXXXXXXX",
    "event_time": 1234567890
}
*/
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = ANY)
public class AppUninstalledSlackEvent implements SlackEventHolder {
    public static final String TYPE = "app_uninstalled";

    private SlackEvent slackEvent;

    public SlackEvent getSlackEvent() {
        return slackEvent;
    }

    public void setSlackEvent(final SlackEvent slackEvent) {
        this.slackEvent = slackEvent;
    }
}
