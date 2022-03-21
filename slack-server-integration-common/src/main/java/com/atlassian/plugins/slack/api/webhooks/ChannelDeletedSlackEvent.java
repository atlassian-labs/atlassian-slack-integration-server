package com.atlassian.plugins.slack.api.webhooks;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import static org.codehaus.jackson.annotate.JsonAutoDetect.Visibility.ANY;

/*
https://api.slack.com/events/channel_archive
{
    "api_app_id": "A66K3L22M66",
    "authorizations": [
        {
            "enterprise_id": null,
            "is_bot": false,
            "is_enterprise_install": false,
            "team_id": "TT0EEPP4R",
            "user_id": "U11PIPLPPSM"
        }
    ],
    "event": {
        "actor_id": "U66NBDDLKPA",
        "channel": "C99DFLK22TY",
        "event_ts": "1633389585.089700",
        "type": "channel_deleted"
    },
    "event_id": "Ee53B8DHG12L",
    "event_time": 1639276685,
    "is_ext_shared_channel": false,
    "team_id": "TT0EEPP4R",
    "token": "dr2FDH3al54fFGWEJHsNdo0u",
    "type": "event_callback"
}

https://api.slack.com/events/group_deleted
{
    "api_app_id": "A66K3L22M66",
    "authorizations": [
        {
            "enterprise_id": null,
            "is_bot": false,
            "is_enterprise_install": false,
            "team_id": "TT0EEPP4R",
            "user_id": "U11PIPLPPSM"
        }
    ],
    "event": {
        "actor_id": "U11PIPLPPSM",
        "channel": "C22Q8LK3SL4",
        "date_deleted": 1619972388,
        "event_ts": "1638975888.000200",
        "type": "group_deleted"
    },

    "event_context": "4-tePlwKR8Ne4dy7XjS7QhOSG7dRVkZgVEBsREJDFMDOJcSYDhfGHrIgKDGHTGDbs2KSf4HdKdVxMqQGD4GkVaAgt4KQEtMSGsDn02Ukw0IilkjW1kSDF6MNBzODk3McZ87H0",
    "event_id": "Es36PQNZWHSP",
    "event_time": 1622937888,
    "is_ext_shared_channel": false,
    "team_id": "TT0EEPP4R",
    "token": "dr2FDH3al54fFGWEJHsNdo0u",
    "type": "event_callback"
}
*/
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = ANY)
public class ChannelDeletedSlackEvent extends BaseChannelEvent {
    public static final String CHANNEL_TYPE = "channel_deleted";
    public static final String GROUP_TYPE = "group_deleted";
}
