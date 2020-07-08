package com.atlassian.plugins.slack.api.events;

import com.atlassian.analytics.api.annotations.EventName;
import com.atlassian.plugins.slack.analytics.AnalyticsContext;

@EventName("notifications.slack.user.updated")
public class SlackUserUpdatedEvent extends BaseAnalyticEvent {
    public SlackUserUpdatedEvent(final AnalyticsContext context) {
        super(context);
    }
}
