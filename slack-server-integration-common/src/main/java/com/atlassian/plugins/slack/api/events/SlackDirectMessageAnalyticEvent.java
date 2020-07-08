package com.atlassian.plugins.slack.api.events;

import com.atlassian.analytics.api.annotations.EventName;
import com.atlassian.plugins.slack.analytics.AnalyticsContext;

@EventName("notifications.slack.inbound.direct.message")
public class SlackDirectMessageAnalyticEvent extends BaseAnalyticEvent {
    public SlackDirectMessageAnalyticEvent(final AnalyticsContext context) {
        super(context);
    }
}
