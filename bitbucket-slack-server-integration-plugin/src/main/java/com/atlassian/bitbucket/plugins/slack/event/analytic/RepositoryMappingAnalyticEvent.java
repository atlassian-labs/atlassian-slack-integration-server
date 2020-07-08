package com.atlassian.bitbucket.plugins.slack.event.analytic;

import com.atlassian.plugins.slack.analytics.AnalyticsContext;
import com.atlassian.plugins.slack.api.events.BaseAnalyticEvent;
import com.atlassian.plugins.slack.util.DigestUtil;
import lombok.Getter;

public abstract class RepositoryMappingAnalyticEvent extends BaseAnalyticEvent {
    @Getter
    protected final int repositoryId;
    protected final String channelId;

    public RepositoryMappingAnalyticEvent(final AnalyticsContext context,
                                          final int repositoryId,
                                          final String channelId) {
        super(context);
        this.repositoryId = repositoryId;
        this.channelId = channelId;
    }

    public long getChannelIdHash() {
        return DigestUtil.crc32(channelId);
    }
}
