package com.atlassian.plugins.slack.api.webhooks;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;

/*
https://api.slack.com/events/app_uninstalled
{
    "api_app_id": "A00T0R11P66",
    "event": {
        "event_ts": "1633245770.103223",
        "type": "app_uninstalled"
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
