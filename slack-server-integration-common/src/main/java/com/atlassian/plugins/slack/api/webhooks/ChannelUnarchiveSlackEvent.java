package com.atlassian.plugins.slack.api.webhooks;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import static org.codehaus.jackson.annotate.JsonAutoDetect.Visibility.ANY;

/*
https://api.slack.com/events/channel_unarchive
{
    "token": "XXXXXXXXXXXXXYYYYYYYYYYY",
    "team_id": "TXXXXXXXX",
    "api_app_id": "AXXXXXXXX",
    "event": {
        "type": "channel_unarchive",
        "channel": "CGLHJQR8F",
        "user": "UFXXXXXXX",
        "event_ts": "1551444306.021500"
    },
    "type": "event_callback",
    "event_id": "EvGL0LSRMF",
    "event_time": 1551444306,
    "authed_users": [
        "UFXXXXXXX"
    ]
}

https://api.slack.com/events/group_unarchive
{
    "token": "XXXXXXXXXXXXXYYYYYYYYYYY",
    "team_id": "TXXXXXXXX",
    "api_app_id": "AG5P6LKFZ",
    "event": {
        "type": "group_unarchive",
        "channel": "GXXXXXXXX",
        "actor_id": "UFXXXXXXX",
        "event_ts": "1551444418.000300"
    },
    "type": "event_callback",
    "event_id": "EvGL0N5Y0Z",
    "event_time": 1551444418,
    "authed_users": [
        "UFXXXXXXX"
    ]
}
*/
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = ANY)
public class ChannelUnarchiveSlackEvent extends BaseChannelEvent {
    public static final String CHANNEL_TYPE = "channel_unarchive";
    public static final String GROUP_TYPE = "group_unarchive";
}
