package com.atlassian.plugins.slack.api.webhooks.action;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/*
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
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BlockKitAction {
    private String type;
    private String value;

    @JsonCreator
    public BlockKitAction(@JsonProperty("type") final String type,
                          @JsonProperty("value") final String value) {
        this.type = type;
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }
}
