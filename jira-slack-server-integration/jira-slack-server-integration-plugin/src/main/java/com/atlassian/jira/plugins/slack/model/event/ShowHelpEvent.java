package com.atlassian.jira.plugins.slack.model.event;

import jakarta.annotation.Nullable;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = true)
public class ShowHelpEvent extends JiraCommandEvent {
    public static final String COMMAND = "help";
    String botUserId;
    String commandName;

    public ShowHelpEvent(final String botUserId, @Nullable final String commandName) {
        super(COMMAND);
        this.botUserId = botUserId;
        this.commandName = commandName;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitShowHelpEvent(this);
    }
}
