package com.atlassian.jira.plugins.slack.model.event;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugins.slack.model.DedicatedChannel;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Event created when we want to show an issue summary in a channel
 */
public class ShowIssueEvent extends JiraCommandEvent {
    public static final String COMMAND = "show";
    private final Issue issue;
    private final DedicatedChannel dedicatedChannel;

    public ShowIssueEvent(final Issue issue, @Nullable final DedicatedChannel dedicatedChannel) {
        super(COMMAND);
        this.issue = issue;
        this.dedicatedChannel = dedicatedChannel;
    }

    public Issue getIssue() {
        return issue;
    }

    public Optional<DedicatedChannel> getDedicatedChannel() {
        return Optional.ofNullable(dedicatedChannel);
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitShowIssueEvent(this);
    }
}
