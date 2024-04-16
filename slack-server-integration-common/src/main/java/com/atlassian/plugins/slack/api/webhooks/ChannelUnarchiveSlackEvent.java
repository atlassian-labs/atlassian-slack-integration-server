package com.atlassian.plugins.slack.api.webhooks;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;

/*
https://api.slack.com/events/channel_unarchive
{
    "api_app_id": "A00T0R11P66",
    "authorizations": [
        {
            "enterprise_id": null,
            "is_bot": true,
            "is_enterprise_install": false,
            "team_id": "TT0EEPP4R",
            "user_id": "U11PIPLPPSM"
        }
    ],
    "event": {
        "channel": "C10P0P00303",
        "channel_type": "group",
        "event_ts": "1020050100.110160",
        "subtype": "channel_unarchive",
        "text": "un-archived the channel",
        "ts": "1080020003.110120",
        "type": "message",
        "user": "U00TPRPIPPA"
    },
    "event_context": "4-tePlwKR8Ne2dy7XjS7QhOSG7dRVkZgVEBsKSHDNVBCJcSYDhfGHrIgKLSMNMSbs2KSf4HdKdVxMqPGD4GkVaAgt4KQEtMSGsDn0",
    "event_id": "Es89PQNRWHSP",
    "event_time": 1638964093,
    "is_ext_shared_channel": false,
    "team_id": "TR6SHGD8R",
    "token": "dr2FDH3al54fFGWEJHsNdo0u",
    "type": "event_callback"
}

https://api.slack.com/events/group_unarchive
{
    "api_app_id": "A55V2K00B66",
    "authorizations": [
        {
            "enterprise_id": null,
            "is_bot": false,
            "is_enterprise_install": false,
            "team_id": "TT0EEPP4R",
            "user_id": "U44GFKJSDPA"
        }
    ],
    "event": {
        "channel": "C43J4N22303",
        "event_ts": "1626448873.013000",
        "type": "group_unarchive",
        "user": "U44GFKJSDPA"
    },
    "event_context": "4-fdXsgVC3Mv3kl2DsHBbb3NV4aJK78fSDFcKLFDBMNSFkSDFhhEWcOgKGNBMVHshh6FO5LsVaRrCqY7IokfNmAdf6KLJaBNCsHa0",
    "event_id": "Ed56SD99KLJB",
    "event_time": 1638964093,
    "is_ext_shared_channel": false,
    "team_id": "TT0EEPP4R",
    "token": "dk2JJK2vb87dKLDFGHaTcc7u",
    "type": "event_callback"
}
*/
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = ANY)
public class ChannelUnarchiveSlackEvent extends BaseChannelEvent {
    public static final String CHANNEL_TYPE = "channel_unarchive";
    public static final String GROUP_TYPE = "group_unarchive";
}
