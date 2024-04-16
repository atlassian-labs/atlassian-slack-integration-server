package com.atlassian.jira.plugins.slack.model.mentions;

import com.atlassian.jira.plugins.slack.model.ChannelKey;
import com.atlassian.jira.plugins.slack.storage.cache.CacheableEntity;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.seratch.jslack.api.model.Conversation;

import javax.annotation.Nonnull;
import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An Issue Mention Channel
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = ANY)
public class MentionChannel implements CacheableEntity<ChannelKey> {
    private final ChannelKey key;
    private final Conversation conversation;
    private final String teamName;

    public MentionChannel(@Nonnull final ChannelKey key,
                          @Nonnull final Conversation conversation,
                          @Nonnull final String teamName) {
        this.key = checkNotNull(key);
        this.conversation = checkNotNull(conversation);
        this.teamName = checkNotNull(teamName);
    }

    public String getTeamName() {
        return teamName;
    }

    @Override
    public ChannelKey getKey() {
        return key;
    }

    public Conversation getConversation() {
        return conversation;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof MentionChannel)) return false;
        final MentionChannel that = (MentionChannel) o;
        return key.equals(that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }
}
