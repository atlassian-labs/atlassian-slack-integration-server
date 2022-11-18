package com.atlassian.confluence.plugins.slack.spacetochannel.events.analytic;

import com.atlassian.analytics.api.annotations.EventName;
import com.atlassian.plugins.slack.analytics.AnalyticsContext;

@EventName("notifications.slack.spacetochannel.linked")
public class SpaceToChannelLinkedAnalyticEvent extends SpaceToChannelAnalyticEvent {
    public SpaceToChannelLinkedAnalyticEvent(final AnalyticsContext context,
                                             final long spaceId,
                                             final String channelId) {
        super(context, spaceId, channelId);
    }
}
