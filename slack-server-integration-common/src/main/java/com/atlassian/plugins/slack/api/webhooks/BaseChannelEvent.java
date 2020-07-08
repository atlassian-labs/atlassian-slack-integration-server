package com.atlassian.plugins.slack.api.webhooks;

import org.apache.commons.lang3.ObjectUtils;
import org.codehaus.jackson.annotate.JsonProperty;

public class BaseChannelEvent implements SlackEventHolder {
    private SlackEvent slackEvent;
    private String channel;
    private String user;
    @JsonProperty("actor_id")
    private String actorId;

    @Override
    public SlackEvent getSlackEvent() {
        return slackEvent;
    }

    @Override
    public void setSlackEvent(final SlackEvent slackEvent) {
        this.slackEvent = slackEvent;
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

    public String getActorId() {
        return actorId;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
    }

    public String getInitiator() {
        return ObjectUtils.firstNonNull(user, actorId);
    }
}
