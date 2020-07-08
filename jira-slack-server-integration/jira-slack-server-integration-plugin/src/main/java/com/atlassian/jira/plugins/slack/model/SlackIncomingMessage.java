package com.atlassian.jira.plugins.slack.model;

import com.atlassian.plugins.slack.api.SlackLink;

import java.util.List;

public class SlackIncomingMessage {
    private String teamId;
    private SlackLink slackLink;
    private final String channelId;
    private final String text;
    private final String previousText;
    private final String ts;
    private final String threadTs;
    private final String user;
    private final String responseUrl;
    private final boolean isMessageEdit;
    private final boolean isLinkShared;
    private final boolean isSlashCommand;
    private final List<String> links;

    public SlackIncomingMessage(
            final String teamId,
            final SlackLink slackLink,
            final String channelId,
            final String text,
            final String previousText,
            final String ts,
            final String threadTs,
            final String user,
            final String responseUrl,
            final boolean isMessageEdit,
            final boolean isLinkShared,
            final boolean isSlashCommand,
            final List<String> links) {
        this.teamId = teamId;
        this.slackLink = slackLink;
        this.channelId = channelId;
        this.text = text;
        this.previousText = previousText;
        this.ts = ts;
        this.threadTs = threadTs;
        this.user = user;
        this.responseUrl = responseUrl;
        this.isMessageEdit = isMessageEdit;
        this.isLinkShared = isLinkShared;
        this.isSlashCommand = isSlashCommand;
        this.links = links;
    }

    public String getTeamId() {
        return teamId;
    }

    public SlackLink getSlackLink() {
        return slackLink;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getText() {
        return text;
    }

    public String getPreviousText() {
        return previousText;
    }

    public String getTs() {
        return ts;
    }

    public String getThreadTs() {
        return threadTs;
    }

    public String getUser() {
        return user;
    }

    public String getResponseUrl() {
        return responseUrl;
    }

    public boolean isMessageEdit() {
        return isMessageEdit;
    }

    public boolean isLinkShared() {
        return isLinkShared;
    }

    public boolean isSlashCommand() {
        return isSlashCommand;
    }

    public List<String> getLinks() {
        return links;
    }
}
