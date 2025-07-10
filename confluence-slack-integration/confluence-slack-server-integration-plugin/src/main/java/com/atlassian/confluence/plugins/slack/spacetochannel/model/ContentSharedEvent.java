package com.atlassian.confluence.plugins.slack.spacetochannel.model;

import com.atlassian.confluence.plugins.slack.spacetochannel.api.notifications.ConfluenceSlackEvent;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.user.ConfluenceUser;
import com.github.seratch.jslack.api.model.Attachment;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;

public class ContentSharedEvent implements ConfluenceSlackEvent, ChannelContext {
    private final Space space;
    private final String teamId;
    private final String channelId;
    private final String threadTs;
    private final Attachment attachment;

    public ContentSharedEvent(final Space space,
                              final String teamId,
                              final String channelId,
                              final String threadTs,
                              final Attachment attachment) {
        this.space = space;
        this.teamId = teamId;
        this.channelId = channelId;
        this.threadTs = threadTs;
        this.attachment = attachment;
    }

    @Override
    public Space getSpace() {
        return space;
    }

    @Override
    public ConfluenceUser getUser() {
        return null;
    }

    @Override
    public String getLink() {
        return "";
    }

    public Attachment getAttachment() {
        return attachment;
    }

    @Nonnull
    @Override
    public String getTeamId() {
        return teamId;
    }

    @Nonnull
    @Override
    public String getChannelId() {
        return channelId;
    }

    @Nullable
    @Override
    public String getThreadTs() {
        return threadTs;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof ContentSharedEvent)) return false;
        final ContentSharedEvent that = (ContentSharedEvent) o;
        return Objects.equals(space, that.space) &&
                Objects.equals(teamId, that.teamId) &&
                Objects.equals(channelId, that.channelId) &&
                Objects.equals(threadTs, that.threadTs) &&
                Objects.equals(attachment, that.attachment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(space, teamId, channelId, threadTs, attachment);
    }

    @Override
    public String toString() {
        return "ContentSharedEvent{" +
                "space=" + space +
                ", teamId='" + teamId + '\'' +
                ", channelId='" + channelId + '\'' +
                ", threadTs='" + threadTs + '\'' +
                ", attachment=" + attachment +
                '}';
    }
}
