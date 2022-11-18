package com.atlassian.confluence.plugins.slack.spacetochannel.events;

import com.atlassian.confluence.plugins.slack.spacetochannel.configuration.SlackChannelDefinition;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.plugins.slack.util.DigestUtil;
import com.atlassian.sal.api.user.UserKey;

import java.util.Objects;
import java.util.Optional;

/**
 * Supertype for all events triggered when an event related to Slack Space-to-Channel configuration happens
 */
public abstract class SpaceToChannelConfigEvent {
    private final Space space;
    private final SlackChannelDefinition channel;
    private final ConfluenceUser user;

    public SpaceToChannelConfigEvent(final Space space, final SlackChannelDefinition channel, final ConfluenceUser user) {
        this.space = space;
        this.channel = channel;
        this.user = user;
    }

    public Space getSpace() {
        return space;
    }

    public long getSpaceId() {
        return space.getId();
    }

    public SlackChannelDefinition getChannel() {
        return channel;
    }

    public long getChannelIdHash() {
        return Optional.ofNullable(channel)
                .map(SlackChannelDefinition::getChannelId)
                .map(DigestUtil::crc32)
                .orElse(0L);
    }

    public long getTeamIdHash() {
        return Optional.ofNullable(channel)
                .map(SlackChannelDefinition::getTeamId)
                .map(DigestUtil::crc32)
                .orElse(0L);
    }

    public ConfluenceUser getUser() {
        return user;
    }

    public long getUserKeyHash() {
        return Optional.ofNullable(user)
                .map(ConfluenceUser::getKey)
                .map(UserKey::getStringValue)
                .map(DigestUtil::crc32)
                .orElse(0L);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof SpaceToChannelConfigEvent)) return false;
        final SpaceToChannelConfigEvent that = (SpaceToChannelConfigEvent) o;
        return Objects.equals(space, that.space) &&
                Objects.equals(channel, that.channel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(space, channel);
    }

    @Override
    public String toString() {
        return "SpaceToChannelConfigEvent{" +
                "space=" + space +
                ", channel=" + channel +
                '}';
    }
}
