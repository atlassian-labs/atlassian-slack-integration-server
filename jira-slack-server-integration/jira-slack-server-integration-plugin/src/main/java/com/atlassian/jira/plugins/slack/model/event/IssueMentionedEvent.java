package com.atlassian.jira.plugins.slack.model.event;

import com.atlassian.jira.plugins.slack.model.SlackIncomingMessage;

/**
 * Event created when we want to report a mention of an issue to the issue's dedicated channel
 */
public class IssueMentionedEvent extends JiraCommandEvent {
    public static final String COMMAND = "show";
    private final SlackIncomingMessage message;
    private final long issueId;

    public IssueMentionedEvent(final SlackIncomingMessage message, final long issueId) {
        super(COMMAND);
        this.message = message;
        this.issueId = issueId;
    }

    public SlackIncomingMessage getMessage() {
        return message;
    }

    public long getIssueId() {
        return issueId;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitIssueMentionedEvent(this);
    }

    @Override
    public String toString() {
        return "IssueMentionedEvent{" +
                "from=" + message.getUser() +
                ", channel=" + message.getChannelId() +
                ", message='" + message.getText() + '\'' +
                ", issueId=" + issueId +
                '}';
    }
}
