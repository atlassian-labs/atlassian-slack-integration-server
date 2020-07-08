package com.atlassian.plugins.slack.api.webhooks;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import static org.codehaus.jackson.annotate.JsonAutoDetect.Visibility.ANY;

/*
https://api.slack.com/events/channel_archive
{
    "token": "XXXXXXXXXXXXXYYYYYYYYYYY",
    "team_id": "TXXXXXXXX",
    "api_app_id": "AXXXXXXXX",
    "event": {
        "type": "channel_archive",
        "channel": "CGLHJQR8F",
        "user": "UFXXXXXXX",
        "is_moved": 0,
        "event_ts": "1551444460.022100"
    },
    "type": "event_callback",
    "event_id": "EvGL0NLP97",
    "event_time": 1551444460,
    "authed_users": [
        "UFXXXXXXX"
    ]
}

https://api.slack.com/events/group_archive
{
    "token": "XXXXXXXXXXXXXYYYYYYYYYYY",
    "team_id": "TXXXXXXXX",
    "api_app_id": "AXXXXXXXX",
    "event": {
        "type": "group_archive",
        "channel": "GXXXXXXXX",
        "is_moved": 0,
        "actor_id": "UFXXXXXXX",
        "event_ts": "1551444358.000100"
    },
    "type": "event_callback",
    "event_id": "EvGLHJCFBL",
    "event_time": 1551444358,
    "authed_users": [
        "UFXXXXXXX"
    ]
}
*/
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = ANY)
public class ChannelArchiveSlackEvent extends BaseChannelEvent {
    public static final String CHANNEL_TYPE = "channel_archive";
    public static final String GROUP_TYPE = "group_archive";
}
