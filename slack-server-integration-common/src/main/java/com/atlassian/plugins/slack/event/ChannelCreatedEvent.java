package com.atlassian.plugins.slack.event;

import com.atlassian.analytics.api.annotations.EventName;
import com.atlassian.plugins.slack.analytics.AnalyticsContext;
import com.atlassian.plugins.slack.api.events.BaseAnalyticEvent;
import com.atlassian.plugins.slack.util.DigestUtil;

@EventName("notifications.slack.channel.created")
public class ChannelCreatedEvent extends BaseAnalyticEvent {
    private final String channelId;
    private final boolean isPrivate;

    public ChannelCreatedEvent(final AnalyticsContext context,
                               final String channelId,
                               final boolean isPrivate) {
        super(context);
        this.channelId = channelId;
        this.isPrivate = isPrivate;
    }

    public long getChannelIdHash() {
        return DigestUtil.crc32(channelId);
    }

    public boolean isPrivate() {
        return isPrivate;
    }
}
