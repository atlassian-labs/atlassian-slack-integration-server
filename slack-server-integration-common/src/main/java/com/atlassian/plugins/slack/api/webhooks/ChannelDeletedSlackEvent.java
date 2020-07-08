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
        "type": "channel_deleted",
        "channel": "CGLHJQR8F",
        "actor_id": "UFXXXXXXX",
        "event_ts": "1551444515.022700"
    },
    "type": "event_callback",
    "event_id": "EvGLGA6JE8",
    "event_time": 1551444515,
    "authed_users": [
        "UFXXXXXXX"
    ]
}

https://api.slack.com/events/group_deleted
{
    "token": "XXXXXXXXXXXXXYYYYYYYYYYY",
    "team_id": "TXXXXXXXX",
    "api_app_id": "AXXXXXXXX",
    "event": {
        "type": "group_deleted",
        "channel": "GXXXXXXXX",
        "date_deleted": 1551444591,
        "actor_id": "UFXXXXXXX",
        "event_ts": "1551444591.000500"
    },
    "type": "event_callback",
    "event_id": "EvGLT5UN21",
    "event_time": 1551444591,
    "authed_users": [
        "UFXXXXXXX"
    ]
}
*/
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = ANY)
public class ChannelDeletedSlackEvent extends BaseChannelEvent {
    public static final String CHANNEL_TYPE = "channel_deleted";
    public static final String GROUP_TYPE = "group_deleted";
}
