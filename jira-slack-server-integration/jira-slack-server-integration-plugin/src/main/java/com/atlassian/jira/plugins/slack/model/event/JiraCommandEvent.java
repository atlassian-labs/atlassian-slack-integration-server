package com.atlassian.jira.plugins.slack.model.event;

import lombok.Value;
import lombok.experimental.NonFinal;

/**
 * Event that will handle the rendering of any command
 */
@Value
@NonFinal
public abstract class JiraCommandEvent implements PluginEvent {
    public interface Visitor<T> {
        T visitIssueMentionedEvent(IssueMentionedEvent event);

        T visitShowIssueEvent(ShowIssueEvent event);

        T visitShowIssueNotFoundEvent(ShowIssueNotFoundEvent event);

        T visitShowHelpEvent(ShowHelpEvent event);

        T visitShowWelcomeEvent(ShowWelcomeEvent event);

        T visitShowBotAddedHelpEvent(ShowBotAddedHelpEvent event);

        T visitShowAccountInfoEvent(ShowAccountInfoEvent event);
    }

    String commandType;

    public abstract <T> T accept(Visitor<T> visitor);
}
