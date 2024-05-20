package com.atlassian.plugins.slack.api.webhooks.action;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/*
{
  "type": "dialog_submission",
  "token": "1EFfUPaOAXbmaTBGIKfD0KLE",
  "action_ts": "1557504882.399612",
  "team": {
    "id": "TC3NTTN4R",
    "domain": "pi-devs"
  },
  "user": {
    "id": "UF46A66P2",
    "name": "mvlasov"
  },
  "channel": {
    "id": "GGN0XTQ6S",
    "name": "privategroup"
  },
  "submission": {
    "comment": "something"
  },
  "callback_id": "{\"objectType\":\"pullRequest\",\"objectId\":\"10\",\"repositoryId\":12,\"commentId\":106}",
  "response_url": "https:\/\/hooks.slack.com\/app\/TC3NTTN4R\/632247559840\/gN4XWdFM2P08NNvL8w8u7Vey",
  "state": ""
}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DialogSubmissionSlackAction extends SlackAction {
    public static final String TYPE = "dialog_submission";

    private String callbackId;
    private Map<String, Object> submission;

    @JsonCreator
    public DialogSubmissionSlackAction(@JsonProperty("type") final String type,
                                       @JsonProperty("callback_id") final String callbackId,
                                       @JsonProperty("submission") final Map<String, Object> submission) {
        super(type);
        this.callbackId = callbackId;
        this.submission = submission;
    }

    public Map<String, Object> getSubmission() {
        return submission;
    }

    public String getCallbackId() {
        return callbackId;
    }
}
