package com.atlassian.plugins.slack.api.events;

import com.atlassian.analytics.api.annotations.EventName;
import com.atlassian.plugins.slack.analytics.AnalyticsContext;

@EventName("notifications.slack.team.unregistered")
public class SlackWorkspaceUnregisteredEvent extends BaseAnalyticEvent {
    public SlackWorkspaceUnregisteredEvent(final AnalyticsContext context) {
        super(context);
    }
}
