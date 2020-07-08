package com.atlassian.jira.plugins.slack.model.event;

public class ShowWelcomeEvent extends JiraCommandEvent {
    private static final String COMMAND = "welcome";

    private final String teamId;

    public ShowWelcomeEvent(final String teamId) {
        super(COMMAND);
        this.teamId = teamId;
    }

    public String getTeamId() {
        return teamId;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitShowWelcomeEvent(this);
    }
}
