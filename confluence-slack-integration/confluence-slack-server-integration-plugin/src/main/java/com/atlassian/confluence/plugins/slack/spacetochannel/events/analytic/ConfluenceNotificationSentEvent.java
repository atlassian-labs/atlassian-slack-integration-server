package com.atlassian.confluence.plugins.slack.spacetochannel.events.analytic;

import com.atlassian.analytics.api.annotations.EventName;
import com.atlassian.plugins.slack.analytics.AnalyticsContext;
import com.atlassian.plugins.slack.api.events.BaseAnalyticEvent;
import lombok.Getter;

public class ConfluenceNotificationSentEvent extends BaseAnalyticEvent {
    public enum Type {
        REGULAR, PERSONAL, UNFURLING;
    }

    @Getter
    private final String notificationKey;
    private final Type type;

    public ConfluenceNotificationSentEvent(final AnalyticsContext context,
                                           final String notificationKey,
                                           final Type type) {
        super(context);
        this.notificationKey = notificationKey;
        this.type = type;
    }

    @EventName
    public String getName() {
        return "notifications.slack.notification." + type.name().toLowerCase() + ".sent";
    }
}
