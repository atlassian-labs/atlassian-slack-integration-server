package com.atlassian.plugins.slack.api.webhooks;

import lombok.Data;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

/*
https://api.slack.com/events/link_shared
{
    "token": "XXYYZZ",
    "team_id": "TXXXXXXXX",
    "api_app_id": "AXXXXXXXXX",
    "event": {
        "type": "link_shared",
        "channel": "Cxxxxxx",
        "user": "Uxxxxxxx",
        "message_ts": "123456789.9875",
        "thread_ts": "123456621.1855",
        "links": [
            {
                "domain": "example.com",
                "url": "https://example.com/12345"
            },
            {
                "domain": "example.com",
                "url": "https://example.com/67890"
            },
            {
                "domain": "another-example.com",
                "url": "https://yet.another-example.com/v/abcde"
            }
        ]
    },
    "type": "event_callback",
    "authed_users": [
        "UXXXXXXX1",
        "UXXXXXXX2"
    ],
    "event_id": "Ev08MFMKH6",
    "event_time": 123456789
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
