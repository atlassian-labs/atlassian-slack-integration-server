package com.atlassian.plugins.slack.api.events;

import com.atlassian.analytics.api.annotations.EventName;
import com.atlassian.plugins.slack.analytics.AnalyticsContext;
import lombok.Getter;

public class PersonalNotificationConfiguredEvent extends BaseAnalyticEvent {
    @Getter
    private final String notificationKey;
    private final boolean enabled;

    public PersonalNotificationConfiguredEvent(final AnalyticsContext context,
                                               final String notificationKey,
                                               final boolean enabled) {
        super(context);
        this.notificationKey = notificationKey;
        this.enabled = enabled;
    }

    @EventName
    public String getName() {
        return "notifications.slack.personal.notification." + (enabled ? "enabled" : "disabled");
    }
}
