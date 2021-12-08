package com.atlassian.plugins.slack.api.webhooks;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import static org.codehaus.jackson.annotate.JsonAutoDetect.Visibility.ANY;

/*
https://api.slack.com/events/channel_archive
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
        "event_ts": "1020050100.110110",
        "subtype": "channel_archive",
        "text": "archived the channel",
        "ts": "1080020003.110010",
        "type": "message",
        "user": "U00TPRPIPPA"
    },
    "event_context": "4-tePlwKR8Ne4dy7XjS7QhOSG7dRVkZgVEBsREJDFMDOJcSYDhfGHrIgKDGHTGDbs2KSf4HdKdVxMqQGD4GkVaAgt4KQEtMSGsDn0",
    "event_id": "Es36PQNZWHSP",
    "event_time": 1642567283,
    "is_ext_shared_channel": false,
    "team_id": "TR6SHGD8R",
    "token": "dr2FDH3al54fFGWEJHsNdo0u",
    "type": "event_callback"
}

https://api.slack.com/events/group_archive
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
        "event_ts": "1626448873.040100",
        "is_moved": 0,
        "type": "group_archive",
        "user": "U44GFKJSDPA"
    },
    "event_context": "4-fdXsgVC3Mv3kl2DsK8GaBNV4aKLxOiSDFcKLFDBMNSFkSDFhhEWcOgKGNBMVHss6VKs5LsVaRrCqJKH3DfNmAdf6KLJaBNCsHa0",
    "event_id": "Ed56SD99KLJB",
    "event_time": 1638955773,
    "is_ext_shared_channel": false,
    "team_id": "TT0EEPP4R",
    "token": "dk2JJK2vb87dKLDFGHaTcc7u",
    "type": "event_callback"
}
*/
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = ANY)
public class ChannelArchiveSlackEvent extends BaseChannelEvent {
    public static final String CHANNEL_TYPE = "channel_archive";
    public static final String GROUP_TYPE = "group_archive";
}
