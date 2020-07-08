package com.atlassian.bitbucket.plugins.slack.event.analytic;

import com.atlassian.analytics.api.annotations.EventName;
import com.atlassian.plugins.slack.analytics.AnalyticsContext;

@EventName("notifications.slack.repotochannel.linked")
public class RepositoryLinkedAnalyticEvent extends RepositoryMappingAnalyticEvent {
    public RepositoryLinkedAnalyticEvent(final AnalyticsContext context,
                                         final int repositoryId,
                                         final String channelId) {
        super(context, repositoryId, channelId);
    }
}
