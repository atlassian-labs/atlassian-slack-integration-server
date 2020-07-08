package com.atlassian.jira.plugins.slack.model.event;

public class ShowIssueNotFoundEvent extends JiraCommandEvent {
    private static final String COMMAND = "issueNotFound";

    public ShowIssueNotFoundEvent() {
        super(COMMAND);
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitShowIssueNotFoundEvent(this);
    }
}
