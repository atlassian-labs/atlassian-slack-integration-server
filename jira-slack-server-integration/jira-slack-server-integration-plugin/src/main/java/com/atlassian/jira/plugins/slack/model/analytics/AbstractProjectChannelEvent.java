package com.atlassian.jira.plugins.slack.model.analytics;

import com.atlassian.plugins.slack.analytics.AnalyticsContext;

public abstract class AbstractProjectChannelEvent extends AbstractChannelEvent {
    private final long projectId;

    public AbstractProjectChannelEvent(final AnalyticsContext context,
                                       final String channelId,
                                       final String owner,
                                       final long projectId) {
        super(context, channelId, owner);
        this.projectId = projectId;
    }

    public long getProjectId() {
        return projectId;
    }
}
