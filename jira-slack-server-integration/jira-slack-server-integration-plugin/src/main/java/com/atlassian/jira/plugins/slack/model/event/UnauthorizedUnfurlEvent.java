package com.atlassian.jira.plugins.slack.model.event;

import com.atlassian.jira.plugins.slack.model.analytics.AbstractIssueChannelEvent;
import com.atlassian.jira.plugins.slack.model.analytics.AbstractProjectChannelEvent;
import com.atlassian.plugins.slack.analytics.AnalyticsContext;

public class UnauthorizedUnfurlEvent extends AbstractIssueChannelEvent implements ConfigurationEvent {
    public UnauthorizedUnfurlEvent(final AnalyticsContext context,
                                   final Long projectId,
                                   final String issueKey,
                                   final String channelId,
                                   final String owner) {
        super(context, projectId, issueKey, channelId, owner);
    }

    @Override
    public ConfigurationEvent.ConfigurationEventType getEventType() {
        return ConfigurationEvent.ConfigurationEventType.UNAUTHORIZED_UNFURL;
    }

    @Override
    public AbstractProjectChannelEvent getAnalyticsEvent() {
        return null;
    }

    @Override
    public <T> T accept(ConfigurationEvent.Visitor<T> visitor) {
        return visitor.visitUnauthorizedUnfurlConfigurationEvent(this);
    }
}
