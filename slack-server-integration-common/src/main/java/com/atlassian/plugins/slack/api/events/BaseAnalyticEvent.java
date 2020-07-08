package com.atlassian.plugins.slack.api.events;

import com.atlassian.plugins.slack.analytics.AnalyticsContext;
import com.atlassian.plugins.slack.util.DigestUtil;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class BaseAnalyticEvent {
    protected final AnalyticsContext context;

    public long getTeamIdHash() {
        return DigestUtil.crc32(context.getTeamId());
    }

    public long getSlackUserIdHash() {
        return DigestUtil.crc32(context.getSlackUserId());
    }

    public long getUserKeyHash() {
        return DigestUtil.crc32(context.getUserKey());
    }
}
