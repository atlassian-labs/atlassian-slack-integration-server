package com.atlassian.jira.plugins.slack.model.mentions;

import com.atlassian.jira.plugins.slack.model.mentions.cache.MentionMessageKey;
import com.atlassian.jira.plugins.slack.storage.cache.CacheableEntity;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * An Issue Mention Message
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = ANY)
public class MentionMessage implements CacheableEntity<MentionMessageKey> {
    private final String teamId;
    private final String channelId;
    private final String text;
    private final String user;
    private final String ts;

    MentionMessage(
            final String teamId,
            final String channelId,
            final String text,
            final String user,
            final String ts) {
        this.teamId = teamId;
        this.text = text;
        this.user = user;
        this.ts = ts;
        checkArgument(!isNullOrEmpty(channelId));
        this.channelId = channelId;
    }

    public String getTeamId() {
        return teamId;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getText() {
        return text;
    }

    public String getUser() {
        return user;
    }

    @SuppressWarnings("unused")
    public String getTs() {
        return ts;
    }

    @Override
    public MentionMessageKey getKey() {
        return new MentionMessageKey(teamId, channelId, ts);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof MentionMessage)) {
            return false;
        }

        MentionMessage that = (MentionMessage) o;

        return getKey().equals(that.getKey());
    }

    @Override
    public int hashCode() {
        return getKey().hashCode();
    }
}
