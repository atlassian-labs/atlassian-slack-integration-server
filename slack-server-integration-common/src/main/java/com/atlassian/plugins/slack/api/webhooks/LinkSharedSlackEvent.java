package com.atlassian.plugins.slack.api.webhooks;

import lombok.Data;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

/*
https://api.slack.com/events/link_shared
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
        "blocks": [
            {
                "block_id": "+sY",
                "elements": [
                    {
                        "elements": [
                            {
                                "type": "link",
                                "url": "https://www.youtube.com/watch?v=xIOjqTRYZwg"
                            }
                        ],
                        "type": "rich_text_section"
                    }
                ],
                "type": "rich_text"
            }
        ],
        "channel": "C10P0P00303",
        "channel_type": "group",
        "client_msg_id": "fkj3b7s5-2w33-6n78-d33a-b7782s3v62s0",
        "event_ts": "1637673397.000700",
        "team": "TT0EEPP4R",
        "text": "<https://www.youtube.com/watch?v=xIOjqTRYZwg>",
        "ts": "1988231397.000700",
        "type": "message",
        "user": "U00TPRPIPPA"
    },
    "event_context": "4-tePlwKR8Ne4dy7XjS7QhWQL7dRVkZgVEBsREJNBMDOJcSYDhfGHrIgKDGHTGDbs2KSf4HdKdVxMqQGD4GkVaAgt4KQEtMSGsDn0",
    "event_id": "Es36PQNZWHSP",
    "event_time": 1465987397,
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
