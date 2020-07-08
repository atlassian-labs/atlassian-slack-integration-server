package com.atlassian.jira.plugins.slack.model.analytics;

import com.atlassian.analytics.api.annotations.EventName;
import com.atlassian.plugins.slack.analytics.AnalyticsContext;
import com.atlassian.plugins.slack.api.events.BaseAnalyticEvent;
import lombok.Getter;

public class JiraNotificationSentEvent extends BaseAnalyticEvent {
    public enum Type {
        REGULAR, POST_FUNCTION, DEDICATED, PERSONAL, UNFURLING;
    }

    @Getter
    private final String notificationKey;
    private final Type type;

    public JiraNotificationSentEvent(final AnalyticsContext context,
                                     final String notificationKey,
                                     final Type type) {
        super(context);
        this.notificationKey = notificationKey;
        this.type = type;
    }

    @EventName
    public String getName() {
        return "jira.slack.integration.notification." + type.name().toLowerCase() + ".sent";
    }
}
