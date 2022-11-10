package com.atlassian.confluence.plugins.slack.spacetochannel.events.analytic;

import com.atlassian.analytics.api.annotations.EventName;
import com.atlassian.plugins.slack.analytics.AnalyticsContext;

/**
 * Fired when a space-to-channel link is removed, i.e. all notification types for that channel are disabled for this space
 */
@EventName("notifications.slack.spacetochannel.notification.disabled")
public class SpaceToChannelNotificationDisabledAnalyticEvent extends SpaceToChannelNotificationAnalyticEvent {
    public SpaceToChannelNotificationDisabledAnalyticEvent(final AnalyticsContext context,
                                                           final long spaceId,
                                                           final String channelId,
                                                           final String notificationKey) {
        super(context, spaceId, channelId, notificationKey);
    }
}
