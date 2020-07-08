package com.atlassian.bitbucket.plugins.slack.event.analytic;

import com.atlassian.analytics.api.annotations.EventName;
import com.atlassian.event.api.AsynchronousPreferred;
import com.atlassian.plugins.slack.analytics.AnalyticsContext;

@AsynchronousPreferred
@EventName("notifications.slack.repotochannel.notification.disabled")
public class RepositoryNotificationDisabledAnalyticEvent extends RepositoryNotificationAnalyticEvent {
    public RepositoryNotificationDisabledAnalyticEvent(final AnalyticsContext context,
                                                       final int repositoryId,
                                                       final String channelId,
                                                       final String notificationKey) {
        super(context, repositoryId, channelId, notificationKey);
    }
}
