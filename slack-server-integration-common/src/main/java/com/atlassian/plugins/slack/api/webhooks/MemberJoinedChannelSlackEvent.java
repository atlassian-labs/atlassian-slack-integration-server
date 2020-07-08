package com.atlassian.plugins.slack.api.webhooks;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import static org.codehaus.jackson.annotate.JsonAutoDetect.Visibility.ANY;

/*
https://api.slack.com/events/member_joined_channel
{
  "token": "PfadpmYMrCxnqUH0tMl8Fryx",
  "team_id": "TC7SRSB25",
  "api_app_id": "AF7KED0HL",
  "event": {
    "type": "member_joined_channel",
    "user": "UF62SGQP6",
    "channel": "CF662G3JN",
    "channel_type": "C",
    "team": "TC7SRSB25",
    "inviter": "UC7SRSBH7",
    "event_ts": "1547268640.003000"
  },
  "type": "event_callback",
  "event_id": "EvFCC4SB3P",
  "event_time": 1547268640,
  "authed_users": [
    "UF62SGQP6"
  ]
}
*/
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = ANY)
public class MemberJoinedChannelSlackEvent implements SlackEventHolder {
    public static final String TYPE = "member_joined_channel";

    private SlackEvent slackEvent;
    private String user;
    private String channel;
    @JsonProperty("channel_type")
    private String channelType;
    private String team;
    private String inviter;

    public SlackEvent getSlackEvent() {
        return slackEvent;
    }

    public void setSlackEvent(final SlackEvent slackEvent) {
        this.slackEvent = slackEvent;
    }

    public String getUser() {
        return user;
    }

    public void setUser(final String user) {
        this.user = user;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(final String channel) {
        this.channel = channel;
    }

    public String getChannelType() {
        return channelType;
    }

    public void setChannelType(final String channelType) {
        this.channelType = channelType;
    }

    public String getTeam() {
        return team;
    }

    public void setTeam(final String team) {
        this.team = team;
    }

    public String getInviter() {
        return inviter;
    }

    public void setInviter(final String inviter) {
        this.inviter = inviter;
    }
}
