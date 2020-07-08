package com.atlassian.plugins.slack.api.webhooks;

import lombok.Data;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import static org.codehaus.jackson.annotate.JsonAutoDetect.Visibility.ANY;

/*
https://api.slack.com/events/app_home_opened
{
  "token": "XXYYZZ",
  "team_id": "TXXXXXXXX",
  "api_app_id": "AXXXXXXXXX",
  "event": {
    "type": "app_home_opened",
    "user": "UFXXXXXXX",
    "channel": "DLX1Z611B"
  },
  "type": "event_callback",
  "event_id": "EvLX1P57NC",
  "event_time": 1564526855
}
*/
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = ANY)
public class AppHomeOpenedSlackEvent implements SlackEventHolder {
    public static final String TYPE = "app_home_opened";

    private SlackEvent slackEvent;
    private String user;
    private String channel;
}
