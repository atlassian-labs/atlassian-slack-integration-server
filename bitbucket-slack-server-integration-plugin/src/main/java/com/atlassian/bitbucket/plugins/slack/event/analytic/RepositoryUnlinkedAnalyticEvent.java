package com.atlassian.bitbucket.plugins.slack.event.analytic;

import com.atlassian.analytics.api.annotations.EventName;
import com.atlassian.plugins.slack.analytics.AnalyticsContext;

@EventName("notifications.slack.repotochannel.unlinked")
public class RepositoryUnlinkedAnalyticEvent extends RepositoryMappingAnalyticEvent {
    public RepositoryUnlinkedAnalyticEvent(final AnalyticsContext context,
                                           final int repositoryId,
                                           final String channelId) {
        super(context, repositoryId, channelId);
    }
}
