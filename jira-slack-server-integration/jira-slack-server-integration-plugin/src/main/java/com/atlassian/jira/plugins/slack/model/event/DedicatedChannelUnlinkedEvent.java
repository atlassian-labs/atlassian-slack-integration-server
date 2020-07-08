package com.atlassian.jira.plugins.slack.model.event;

import com.atlassian.analytics.api.annotations.EventName;
import com.atlassian.jira.plugins.slack.model.analytics.AbstractIssueChannelEvent;
import com.atlassian.jira.plugins.slack.model.analytics.AbstractProjectChannelEvent;
import com.atlassian.plugins.slack.analytics.AnalyticsContext;

@EventName("jira.slack.integration.dedicatedchannel.unlinked")
public class DedicatedChannelUnlinkedEvent extends AbstractIssueChannelEvent implements ConfigurationEvent {
    public DedicatedChannelUnlinkedEvent(final AnalyticsContext context,
                                         final Long projectId,
                                         final String issueKey,
                                         final String channelId,
                                         final String owner) {
        super(context, projectId, issueKey, channelId, owner);
    }

    @Override
    public AbstractProjectChannelEvent getAnalyticsEvent() {
        return this;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitDedicatedChannelUnlinkedEvent(this);
    }

    @Override
    public ConfigurationEventType getEventType() {
        return ConfigurationEventType.DEDICATED_CHANNEL_UNLINKED;
    }
}
