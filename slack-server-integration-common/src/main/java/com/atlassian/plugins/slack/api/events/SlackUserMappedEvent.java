package com.atlassian.plugins.slack.api.events;

import com.atlassian.analytics.api.annotations.EventName;
import com.atlassian.plugins.slack.analytics.AnalyticsContext;

@EventName("notifications.slack.user.mapped")
public class SlackUserMappedEvent extends BaseAnalyticEvent {
    public SlackUserMappedEvent(final AnalyticsContext context) {
        super(context);
    }
}
