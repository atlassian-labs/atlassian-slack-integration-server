package com.atlassian.plugins.slack.api.webhooks;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/*
https://api.slack.com/events/link_shared
{
    "api_app_id": "A00T0R11P66",
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
        "channel": "C10P0P00303",
        "event_ts": "1638793296.000200",
        "is_bot_user_member": false,
        "links": [
            {
                "domain": "domain.com",
                "url": "https://domain.com"
            }
        ],
        "message_ts": "U77HGNBMCLD-fgvh99ae-f8sd-899c-21bv-3kjlmn44l67g2-9nb33d7kjm0cd3e8796f324jhh4f37f7898796432klj1c324561234d73c6563f",
        "source": "source",
        "type": "link_shared",
        "unfurl_id": "U77HGNBMCLD-fgvh99ae-f8sd-899c-21bv-3kjlmn44l67g2-9nb33d7kjm0cd3e8796f324jhh4f37f7898796432klj1c324561234d73c653f",
        "user": "U00TPRPIPPA"
    },
    "event_context": "4-tePlwKR8Ne4dy7XjS7QhWQL7dRVkZgVEBsREJNBMDOJcSYDhfGHrIgKDGHTGDbs2KSf4HdKdVxMqQGD4GkVaAgt4KQEtMSGsDn0",
    "event_id": "Es36PQNZWHSP",
    "event_time": 1639051134,
    "is_ext_shared_channel": false,
     "team_id": "TT0EEPP4R",
    "token": "dr2FDH3al54fFGWEJHsNdo0u",
    "type": "event_callback"
}
*/
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LinkSharedSlackEvent implements SlackEventHolder {
    public static final String TYPE = "link_shared";

    private SlackEvent slackEvent;
    private String channel;
    private String user;
    @JsonProperty("message_ts")
    private String messageTimestamp;
    @JsonProperty("thread_ts")
    private String threadTimestamp;
    private List<Link> links;

    @Data
    public static class Link {
        String domain;
        String url;
    }
}
