package com.atlassian.plugins.slack.api.webhooks;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import static org.codehaus.jackson.annotate.JsonAutoDetect.Visibility.ANY;

/*
https://api.slack.com/events/member_joined_channel
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
        "channel": "C10P0P00303",
        "channel_type": "C",
        "event_ts": "1638793296.000200",
        "inviter": "U32NBSDOPPA",
        "team": "TT0EEPP4R",
        "type": "member_joined_channel",
        "user": "U00TPRPIPPA"
    },
    "event_context": "4-tePlwKR8Ne4dy7XjS7QhWQL7dRVkZgVEBsREJNBMDOJcSYDhfGHrIgKDGHTGDbs2KSf4HdKdVxMqQGD4GkVaAgt4KQEtMSGsDn0iQzAyNBMVCzSDF6NifQ",
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
