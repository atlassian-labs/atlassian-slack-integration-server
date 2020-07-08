package com.atlassian.plugins.slack.rest.model;

public class DismissUserDisconnectionDto {
    private String slackUserId;

    public DismissUserDisconnectionDto() {
    }

    public String getSlackUserId() {
        return slackUserId;
    }

    public void setSlackUserId(String slackUserId) {
        this.slackUserId = slackUserId;
    }
}
