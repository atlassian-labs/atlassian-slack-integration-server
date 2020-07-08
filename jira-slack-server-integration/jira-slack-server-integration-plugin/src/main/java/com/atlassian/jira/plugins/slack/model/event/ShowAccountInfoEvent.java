package com.atlassian.jira.plugins.slack.model.event;

public class ShowAccountInfoEvent extends JiraCommandEvent {
    public static final String COMMAND = "account";

    private final String slackUserId;

    public ShowAccountInfoEvent(final String slackUserId) {
        super(COMMAND);
        this.slackUserId = slackUserId;
    }

    public String getSlackUserId() {
        return slackUserId;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitShowAccountInfoEvent(this);
    }
}
