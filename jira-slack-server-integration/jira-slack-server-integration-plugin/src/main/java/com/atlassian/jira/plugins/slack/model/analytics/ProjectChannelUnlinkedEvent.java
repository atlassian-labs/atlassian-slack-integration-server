package com.atlassian.jira.plugins.slack.model.analytics;

import com.atlassian.analytics.api.annotations.EventName;
import com.atlassian.plugins.slack.analytics.AnalyticsContext;

@EventName("notifications.slack.projectchannel.unlinked")
public class ProjectChannelUnlinkedEvent extends AbstractProjectChannelEvent {
    public ProjectChannelUnlinkedEvent(final AnalyticsContext context,
                                       final String channelId,
                                       final String owner,
                                       final long projectId) {
        super(context, channelId, owner, projectId);
    }
}
