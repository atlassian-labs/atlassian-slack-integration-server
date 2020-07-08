package com.atlassian.plugins.slack.api.webhooks.action;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

/*
{
  "type": "block_actions",
  "team": {
    "id": "TC3NTTN4R",
    "domain": "pi-devs"
  },
  "user": {
    "id": "UF46A66P2",
    "username": "mvlasov",
    "name": "mvlasov",
    "team_id": "TC3NTTN4R"
  },
  "api_app_id": "AHKTVAVC3",
  "token": "1EFfUPaOAXbmaTBGIKfD0KLE",
  "container": {
    "type": "message",
    "message_ts": "1558648770.002300",
    "channel_id": "GGN0XTQ6S",
    "is_ephemeral": false
  },
  "trigger_id": "644816614352.411775940161.59b70193873dedd9ae26f117f0a22c64",
  "channel": {
    "id": "GGN0XTQ6S",
    "name": "privategroup"
  },
  "message": {
    "type": "message",
    "subtype": "bot_message",
    "text": "This content can't be displayed.",
    "ts": "1558648770.002300",
    "username": "Bitbucket Server Michael Dev",
    "bot_id": "BHXBB7QQ5",
    "blocks": [
      {
        "type": "section",
        "block_id": "OpSss",
        "text": {
          "type": "mrkdwn",
          "text": "Pull request <https:\/\/mvlasov-bitbucket.ngrok.io\/bitbucket\/projects\/PROJECT_1\/repos\/rep_2\/pull-requests\/14|br2 change1> *commented* by <https:\/\/mvlasov-bitbucket.ngrok.io\/bitbucket\/users\/user|User> in <https:\/\/mvlasov-bitbucket.ngrok.io\/bitbucket\/projects\/PROJECT_1\/repos\/rep_2\/browse|Project 1\/rep_2>",
          "verbatim": false
        }
      },
      {
        "type": "actions",
        "block_id": "Kgcv",
        "elements": [
          {
            "type": "button",
            "action_id": "0cN",
            "text": {
              "type": "plain_text",
              "text": "Reply",
              "emoji": true
            },
            "value": "{\"objectType\":\"pullRequest\",\"objectId\":\"14\",\"repositoryId\":12,\"commentId\":243}"
          },
          {
            "type": "button",
            "action_id": "u0P1y",
            "text": {
              "type": "plain_text",
              "text": "View comment",
              "emoji": true
            },
            "url": "https:\/\/mvlasov-bitbucket.ngrok.io\/bitbucket\/projects\/PROJECT_1\/repos\/rep_2\/pull-requests\/14\/overview?commentId=243&actions=reply"
          }
        ]
      }
    ]
  },
  "response_url": "https:\/\/hooks.slack.com\/actions\/TC3NTTN4R\/644816614176\/19TNOxLvCXfRig2K1GbcbpT1",
  "actions": [
    {
      "action_id": "0cN",
      "block_id": "Kgcv",
      "text": {
        "type": "plain_text",
        "text": "Reply",
        "emoji": true
      },
      "value": "{\"objectType\":\"pullRequest\",\"objectId\":\"14\",\"repositoryId\":12,\"commentId\":243}",
      "type": "button",
      "action_ts": "1558648924.711438"
    }
  ]
}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BlockSlackAction extends SlackAction {
    public static final String TYPE = "block_actions";

    private String triggerId;
    private List<BlockKitAction> actions;

    @JsonCreator
    public BlockSlackAction(@JsonProperty("trigger_id") final String triggerId,
                            @JsonProperty("type") final String type,
                            @JsonProperty("actions") final List<BlockKitAction> actions) {
        super(type);
        this.triggerId = triggerId;
        this.actions = actions;
    }

    public String getTriggerId() {
        return triggerId;
    }

    public List<BlockKitAction> getActions() {
        return actions;
    }
}
