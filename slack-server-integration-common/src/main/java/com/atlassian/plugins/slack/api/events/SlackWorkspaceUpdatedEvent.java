package com.atlassian.plugins.slack.api.events;

import com.atlassian.analytics.api.annotations.EventName;
import com.atlassian.plugins.slack.analytics.AnalyticsContext;

@EventName("notifications.slack.team.updated")
public class SlackWorkspaceUpdatedEvent extends BaseAnalyticEvent {
    public SlackWorkspaceUpdatedEvent(final AnalyticsContext context) {
        super(context);
    }
}
