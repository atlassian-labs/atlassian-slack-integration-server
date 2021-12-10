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
                "block_id": "HeN",
                "elements": [
                    {
                        "elements": [
                            {
                                "text": "hello",
                                "type": "text"
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
        "event_ts": "1638793296.000200",
        "team": "TT0EEPP4R",
        "text": "hello",
        "ts": "1548324796.000200",
        "type": "message",
        "user": "U00TPRPIPPA"
    },
    "event_context": "4-tePlwKR8Ne4dy7XjS7QhWQL7dRVkZgVEBsREJNBMDOJcSYDhfGHrIgKDGHTGDbs2KSf4HdKdVxMqQGD4GkVaAgt4KQEtMSGsDn0",
    "event_id": "Es36PQNZWHSP",
    "event_time": 1634653566,
    "is_ext_shared_channel": false,
    "team_id": "TT0EEPP4R",
    "token": "dr2FDH3al54fFGWEJHsNdo0u",
    "type": "event_callback"
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
