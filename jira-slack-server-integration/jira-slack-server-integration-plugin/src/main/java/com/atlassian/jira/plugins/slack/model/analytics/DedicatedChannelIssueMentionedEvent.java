package com.atlassian.jira.plugins.slack.model.analytics;

import com.atlassian.analytics.api.annotations.EventName;
import com.atlassian.plugins.slack.analytics.AnalyticsContext;
import com.atlassian.plugins.slack.api.events.BaseAnalyticEvent;
import com.atlassian.plugins.slack.util.DigestUtil;
import lombok.Getter;

@EventName("jira.slack.integration.dedicatedchannel.issue.mentioned")
public class DedicatedChannelIssueMentionedEvent extends BaseAnalyticEvent {
    private final String sourceChannelId;
    private final String destinationChannelId;
    @Getter
    private final long issueId;

    public DedicatedChannelIssueMentionedEvent(final AnalyticsContext context,
                                               final String sourceChannelId,
                                               final String destinationChannelId,
                                               final long issueId) {
        super(context);
        this.sourceChannelId = sourceChannelId;
        this.destinationChannelId = destinationChannelId;
        this.issueId = issueId;
    }

    public long getSourceChannelIdHash() {
        return DigestUtil.crc32(sourceChannelId);
    }

    public long getDestinationChannelIdHash() {
        return DigestUtil.crc32(destinationChannelId);
    }
}
