package com.atlassian.confluence.plugins.slack.spacetochannel.events.analytic;

import com.atlassian.plugins.slack.analytics.AnalyticsContext;
import com.atlassian.plugins.slack.api.events.BaseAnalyticEvent;
import com.atlassian.plugins.slack.util.DigestUtil;
import lombok.Getter;

public abstract class SpaceToChannelAnalyticEvent extends BaseAnalyticEvent {
    @Getter
    private final long spaceId;
    private final String channelId;

    public SpaceToChannelAnalyticEvent(final AnalyticsContext context,
                                       final long spaceId,
                                       final String channelId) {
        super(context);
        this.spaceId = spaceId;
        this.channelId = channelId;
    }

    public long getChannelIdHash() {
        return DigestUtil.crc32(channelId);
    }
}
