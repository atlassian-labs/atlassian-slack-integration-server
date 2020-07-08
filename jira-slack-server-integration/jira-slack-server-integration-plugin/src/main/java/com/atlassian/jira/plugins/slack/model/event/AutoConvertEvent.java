package com.atlassian.jira.plugins.slack.model.event;

import com.atlassian.analytics.api.annotations.EventName;
import com.atlassian.plugins.slack.analytics.AnalyticsContext;
import com.atlassian.plugins.slack.api.events.BaseAnalyticEvent;

public class AutoConvertEvent extends BaseAnalyticEvent {
    public enum Type {
        GLOBAL,
        PROJECT,
        GUESTS
    }

    protected final Type type;
    protected final boolean enabled;

    public AutoConvertEvent(final AnalyticsContext context,
                            final Type type,
                            final boolean enabled) {
        super(context);
        this.type = type;
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @EventName
    public String getAnalyticEventName() {
        return "notifications.slack.autoconvert." + type.name().toLowerCase() + "." + (enabled ? "enabled" : "disabled");
    }
}
