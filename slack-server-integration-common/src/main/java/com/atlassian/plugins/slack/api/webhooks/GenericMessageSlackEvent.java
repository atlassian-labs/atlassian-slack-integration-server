package com.atlassian.plugins.slack.api.webhooks;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import static org.codehaus.jackson.annotate.JsonAutoDetect.Visibility.ANY;

/*
https://api.slack.com/events/message
https://api.slack.com/events/message/message_deleted

https://api.slack.com/events/message.mpim
https://api.slack.com/events/message.im
https://api.slack.com/events/message.groups
https://api.slack.com/events/message.channels
{
  "token": "XXXXXXXXXXXXYYYYYYYYYYYY",
  "team_id": "TXXXXXXXX",
  "api_app_id": "AXXXXXXXX",
  "event": {
    "client_msg_id": "853ce7b3-b7ff-4096-b202-c3bbf65c1e4a",
    "type": "message",
    "text": "Any message",
    "user": "UC7LU9ZGS",
    "ts": "1547500911.010000",
    "channel": "CF49QEJF4",
    "event_ts": "1547500911.010000",
    "channel_type": "channel"
  },
  "type": "event_callback",
  "event_id": "EvFDLP0K7F",
  "event_time": 1547500911,
  "authed_users": [
    "UF36K27BR"
  ]
}

{
  "token": "XXXXXXXXXXXXYYYYYYYYYYYY",
  "team_id": "TXXXXXXXX",
  "api_app_id": "AXXXXXXXX",
  "event": {
    "type": "message",
    "deleted_ts": "1547500911.010000",
    "subtype": "message_deleted",
    "hidden": true,
    "channel": "CF49QEJF4",
    "previous_message": {
      "type": "message",
      "user": "UC7LU9ZGS",
      "text": "Any message",
      "client_msg_id": "853ce7b3-b7ff-4096-b202-c3bbf65c1e4a",
      "ts": "1547500911.010000"
    },
    "event_ts": "1547500951.010100",
    "ts": "1547500951.010100",
    "channel_type": "channel"
  },
  "type": "event_callback",
  "event_id": "EvFDLPHL69",
  "event_time": 1547500951,
  "authed_users": [
    "UF36K27BR"
  ]
}

{
  "token": "XXXXXXXXXXXXYYYYYYYYYYYY",
  "team_id": "TXXXXXXXX",
  "api_app_id": "AXXXXXXXX",
  "event": {
    "type": "message",
    "message": {
      "type": "message",
      "user": "UC7SRSBH7",
      "text": "PT-2 hahahahahaha db dfvbdffvdfvdfva ad asd",
      "client_msg_id": "dad77307-b142-462b-85b6-e9f0b0a6dc34",
      "edited": {
        "user": "UC7SRSBH7",
        "ts": "1546645882.000000"
      },
      "ts": "1546645707.005000"
    },
    "subtype": "message_changed",
    "hidden": true,
    "channel": "CF62ZFGFM",
    "previous_message": {
      "type": "message",
      "user": "UC7SRSBH7",
      "text": "PT-2 hahahahahaha db dfvbdffvdfvdfv",
      "client_msg_id": "dad77307-b142-462b-85b6-e9f0b0a6dc34",
      "edited": {
        "user": "UC7SRSBH7",
        "ts": "1546645740.000000"
      },
      "ts": "1546645707.005000"
    },
    "event_ts": "1546645882.005600",
    "ts": "1546645882.005600",
    "channel_type": "channel"
  },
  "type": "event_callback",
  "event_id": "EvF6E3KFKK",
  "event_time": 1546645882,
  "authed_users": [
    "UF62SGQP6"
  ]
}


{
  "token": "XXXXXXXXXXXXYYYYYYYYYYYY",
  "team_id": "TXXXXXXXX",
  "api_app_id": "AXXXXXXXX",
  "event": {
    "type": "message",
    "text": "You have been removed from #testcreate2 by <@UC7SRSBH7>",
    "user": "USLACKBOT",
    "ts": "1547268549.000100",
    "channel": "DF6DMV4KX",
    "event_ts": "1547268549.000100",
    "channel_type": "im"
  },
  "type": "event_callback",
  "event_id": "EvFBDY20JC",
  "event_time": 1547268549,
  "authed_users": [
    "UF62SGQP6"
  ]
}

{
  "token": "XXXXXXXXXXXXYYYYYYYYYYYY",
  "team_id": "TXXXXXXXX",
  "api_app_id": "AXXXXXXXX",
  "event": {
    "user": "UF62SGQP6",
    "type": "message",
    "subtype": "channel_join",
    "ts": "1547268640.003100",
    "text": "<@UF62SGQP6> has joined the channel",
    "inviter": "UC7SRSBH7",
    "channel": "CF662G3JN",
    "event_ts": "1547268640.003100",
    "channel_type": "channel"
  },
  "type": "event_callback",
  "event_id": "EvFCC4SBGV",
  "event_time": 1547268640,
  "authed_users": [
    "UF62SGQP6"
  ]
}
*/
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = ANY)
public class GenericMessageSlackEvent implements SlackEventHolder {
    public static final String TYPE = "message";

    @JsonIgnore
    private SlackEvent slackEvent;
    private String subtype;
    private String channel;
    private String user;
    private String text;
    private String ts;
    @JsonProperty("thread_ts")
    private String threadTimestamp;
    private boolean hidden;
    @JsonProperty("channel_type")
    private String channelType;
    @JsonProperty
    private ChangedMessage message;
    @JsonProperty("previous_message")
    private ChangedMessage previousMessage;

    @JsonIgnore
    public boolean isChangedEvent() {
        return "message_changed".equals(subtype);
    }

    @JsonIgnore
    public boolean isDeletedEvent() {
        return "message_deleted".equals(subtype);
    }

    public SlackEvent getSlackEvent() {
        return slackEvent;
    }

    public void setSlackEvent(final SlackEvent slackEvent) {
        this.slackEvent = slackEvent;
    }

    public String getSubtype() {
        return subtype;
    }

    public void setSubtype(final String subtype) {
        this.subtype = subtype;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(final String channel) {
        this.channel = channel;
    }

    public String getUser() {
        return user;
    }

    public void setUser(final String user) {
        this.user = user;
    }

    public String getText() {
        return text;
    }

    public void setText(final String text) {
        this.text = text;
    }

    public String getTs() {
        return ts;
    }

    public void setTs(final String ts) {
        this.ts = ts;
    }

    public String getThreadTimestamp() {
        return threadTimestamp;
    }

    public void setThreadTimestamp(final String threadTimestamp) {
        this.threadTimestamp = threadTimestamp;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(final boolean hidden) {
        this.hidden = hidden;
    }

    public String getChannelType() {
        return channelType;
    }

    public void setChannelType(final String channelType) {
        this.channelType = channelType;
    }

    public ChangedMessage getMessage() {
        return message;
    }

    public void setMessage(final ChangedMessage message) {
        this.message = message;
    }

    public ChangedMessage getPreviousMessage() {
        return previousMessage;
    }

    public void setPreviousMessage(final ChangedMessage previousMessage) {
        this.previousMessage = previousMessage;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonAutoDetect(fieldVisibility = ANY)
    public static class ChangedMessage {
        private String user;
        private String text;
        private String ts;

        public String getUser() {
            return user;
        }

        public void setUser(final String user) {
            this.user = user;
        }

        public String getText() {
            return text;
        }

        public void setText(final String text) {
            this.text = text;
        }

        public String getTs() {
            return ts;
        }

        public void setTs(final String ts) {
            this.ts = ts;
        }
    }
}
