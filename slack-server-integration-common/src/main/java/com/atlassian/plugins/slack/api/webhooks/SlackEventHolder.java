package com.atlassian.plugins.slack.api.webhooks;

public interface SlackEventHolder {
    SlackEvent getSlackEvent();

    void setSlackEvent(SlackEvent slackEvent);
}
