package com.atlassian.jira.plugins.slack.model.analytics;

import com.atlassian.analytics.api.annotations.EventName;
import com.atlassian.plugins.slack.analytics.AnalyticsContext;
import com.atlassian.plugins.slack.api.events.BaseAnalyticEvent;
import lombok.Getter;

public class ProjectNotificationConfiguredEvent extends BaseAnalyticEvent {
    @Getter
    private final String notificationKey;
    private final boolean enabled;

    public ProjectNotificationConfiguredEvent(final AnalyticsContext context,
                                              final String notificationKey,
                                              final boolean enabled) {
        super(context);
        this.notificationKey = notificationKey;
        this.enabled = enabled;
    }

    @EventName
    public String getName() {
        return "jira.slack.integration.notification."  + (enabled ? "enabled" : "disabled");
    }
}
