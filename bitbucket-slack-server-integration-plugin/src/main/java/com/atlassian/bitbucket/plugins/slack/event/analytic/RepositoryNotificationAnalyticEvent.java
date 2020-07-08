package com.atlassian.bitbucket.plugins.slack.event.analytic;

import com.atlassian.plugins.slack.analytics.AnalyticsContext;
import lombok.Getter;

public abstract class RepositoryNotificationAnalyticEvent extends RepositoryMappingAnalyticEvent {
    @Getter
    protected final String notificationKey;

    public RepositoryNotificationAnalyticEvent(final AnalyticsContext context,
                                               final int repositoryId,
                                               final String channelId,
                                               final String notificationKey) {
        super(context, repositoryId, channelId);
        this.notificationKey = notificationKey;
    }
}
