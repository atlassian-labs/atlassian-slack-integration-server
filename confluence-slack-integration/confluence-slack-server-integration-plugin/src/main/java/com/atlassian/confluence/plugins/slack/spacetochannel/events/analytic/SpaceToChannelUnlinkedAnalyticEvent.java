package com.atlassian.confluence.plugins.slack.spacetochannel.events.analytic;

import com.atlassian.analytics.api.annotations.EventName;
import com.atlassian.plugins.slack.analytics.AnalyticsContext;

@EventName("notifications.slack.spacetochannel.unlinked")
public class SpaceToChannelUnlinkedAnalyticEvent extends SpaceToChannelAnalyticEvent {
    public SpaceToChannelUnlinkedAnalyticEvent(final AnalyticsContext context,
                                               final long spaceId,
                                               final String channelId) {
        super(context, spaceId, channelId);
    }
}
