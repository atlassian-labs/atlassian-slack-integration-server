package com.atlassian.confluence.plugins.slack.spacetochannel.events.analytic;

import com.atlassian.analytics.api.annotations.EventName;
import com.atlassian.plugins.slack.analytics.AnalyticsContext;

/**
 * Fired when a space is linked to a channel.
 */
@EventName("notifications.slack.spacetochannel.notification.enabled")
public class SpaceToChannelNotificationEnabledAnalyticEvent extends SpaceToChannelNotificationAnalyticEvent {
    public SpaceToChannelNotificationEnabledAnalyticEvent(final AnalyticsContext context,
                                                          final long spaceId,
                                                          final String channelId,
                                                          final String notificationKey) {
        super(context, spaceId, channelId, notificationKey);
    }
}
