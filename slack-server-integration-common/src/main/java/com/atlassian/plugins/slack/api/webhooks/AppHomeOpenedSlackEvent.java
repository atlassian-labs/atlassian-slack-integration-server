package com.atlassian.plugins.slack.api.webhooks;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;

/*
https://api.slack.com/events/app_home_opened
{
    "type": "app_home_opened",
    "user": "U061F7AUR",
    "channel": "D0LAN2Q65",
    "event_ts": "1515449522000016",
    "tab": "home",
    "view": {
        "id": "VPASKP233",
        "team_id": "T21312902",
        "type": "home",
        "blocks": [
           ...
        ],
        "private_metadata": "",
        "callback_id": "",
        "state":{
            ...
        },
        "hash":"1231232323.12321312",
        "clear_on_close": false,
        "notify_on_close": false,
        "root_view_id": "VPASKP233",
        "app_id": "A21SDS90",
        "external_id": "",
        "app_installed_team_id": "T21312902",
        "bot_id": "BSDKSAO2"
    }
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
