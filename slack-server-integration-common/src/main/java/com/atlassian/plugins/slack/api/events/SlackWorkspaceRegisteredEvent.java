package com.atlassian.plugins.slack.api.events;

import com.atlassian.analytics.api.annotations.EventName;
import com.atlassian.plugins.slack.analytics.AnalyticsContext;

@EventName("notifications.slack.team.registered")
public class SlackWorkspaceRegisteredEvent extends BaseAnalyticEvent {
    public SlackWorkspaceRegisteredEvent(final AnalyticsContext context) {
        super(context);
    }
}
