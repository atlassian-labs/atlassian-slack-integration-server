package com.atlassian.jira.plugins.slack.model.analytics;

import com.atlassian.plugins.slack.analytics.AnalyticsContext;
import com.atlassian.plugins.slack.api.events.BaseAnalyticEvent;
import com.atlassian.plugins.slack.util.DigestUtil;

public abstract class AbstractChannelEvent extends BaseAnalyticEvent {
    private final String channelId;
    private final String owner;

    public AbstractChannelEvent(final AnalyticsContext context,
                                final String channelId,
                                final String owner) {
        super(context);
        this.channelId = channelId;
        this.owner = owner;
    }

    public String getChannelId() {
        return channelId;
    }

    public long getChannelIdHash() {
        return DigestUtil.crc32(channelId);
    }

    public String getTeamId() {
        return context.getTeamId();
    }

    public String getOwner() {
        return owner;
    }

    public long getOwnerHash() {
        return DigestUtil.crc32(owner);
    }
}
