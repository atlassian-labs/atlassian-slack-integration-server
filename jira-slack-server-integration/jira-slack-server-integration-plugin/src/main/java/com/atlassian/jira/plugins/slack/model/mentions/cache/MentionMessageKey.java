package com.atlassian.jira.plugins.slack.model.mentions.cache;

/**
 * Key used to store {@link com.atlassian.jira.plugins.slack.model.mentions.MentionMessage} instances in cache
 */
public class MentionMessageKey {
    private final String teamId;
    private final String channelId;
    private final String messageId;

    public MentionMessageKey(String teamId, String channelId, String messageId) {
        this.teamId = teamId;
        this.channelId = channelId;
        this.messageId = messageId;
    }

    public String getTeamId() {
        return teamId;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getMessageId() {
        return messageId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MentionMessageKey)) {
            return false;
        }

        MentionMessageKey that = (MentionMessageKey) o;

        if (!messageId.equals(that.messageId)) {
            return false;
        }
        if (!channelId.equals(that.channelId)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = teamId.hashCode();
        result = 31 * result + channelId.hashCode();
        result = 31 * result + messageId.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "MentionMessageKey{" +
                "teamId='" + teamId + '\'' +
                "channelId='" + channelId + '\'' +
                ", messageId='" + messageId + '\'' +
                '}';
    }
}
