package com.atlassian.jira.plugins.slack.model.analytics;

import com.atlassian.plugins.slack.analytics.AnalyticsContext;
import com.atlassian.plugins.slack.util.DigestUtil;

public abstract class AbstractIssueChannelEvent extends AbstractProjectChannelEvent {
    private final String issueKey;

    public AbstractIssueChannelEvent(final AnalyticsContext context,
                                     final long projectId,
                                     final String issueKey,
                                     final String channelId,
                                     final String owner) {
        super(context, channelId, owner, projectId);
        this.issueKey = issueKey;
    }

    public String getIssueKey() {
        return issueKey;
    }

    public long getIssueKeyHash() {
        return DigestUtil.crc32(issueKey);
    }
}
