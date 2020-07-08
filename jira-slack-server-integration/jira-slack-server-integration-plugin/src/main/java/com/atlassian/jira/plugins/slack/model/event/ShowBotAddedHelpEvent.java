package com.atlassian.jira.plugins.slack.model.event;

import com.atlassian.plugins.slack.api.SlackLink;

public class ShowBotAddedHelpEvent extends JiraCommandEvent {
    private static final String COMMAND = "botAddedHelp";

    private final SlackLink slackLink;
    private final String channelId;

    public ShowBotAddedHelpEvent(final SlackLink slackLink, final String channelId) {
        super(COMMAND);
        this.slackLink = slackLink;
        this.channelId = channelId;
    }

    public SlackLink getSlackLink() {
        return slackLink;
    }

    public String getChannelId() {
        return channelId;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitShowBotAddedHelpEvent(this);
    }
}
