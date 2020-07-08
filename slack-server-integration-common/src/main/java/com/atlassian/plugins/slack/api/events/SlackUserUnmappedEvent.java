package com.atlassian.plugins.slack.api.events;

import com.atlassian.analytics.api.annotations.EventName;
import com.atlassian.plugins.slack.analytics.AnalyticsContext;

@EventName("notifications.slack.user.unmapped")
public class SlackUserUnmappedEvent extends BaseAnalyticEvent {
    public SlackUserUnmappedEvent(final AnalyticsContext context) {
        super(context);
    }
}
