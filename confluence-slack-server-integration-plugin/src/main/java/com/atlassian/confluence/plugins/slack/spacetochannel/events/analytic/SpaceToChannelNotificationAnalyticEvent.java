package com.atlassian.confluence.plugins.slack.spacetochannel.events.analytic;

import com.atlassian.plugins.slack.analytics.AnalyticsContext;
import lombok.Getter;

public abstract class SpaceToChannelNotificationAnalyticEvent extends SpaceToChannelAnalyticEvent {
    @Getter
    private final String notificationKey;

    public SpaceToChannelNotificationAnalyticEvent(final AnalyticsContext context,
                                                   final long spaceId,
                                                   final String channelId,
                                                   final String notificationKey) {
        super(context, spaceId, channelId);
        this.notificationKey = notificationKey;
    }
}
