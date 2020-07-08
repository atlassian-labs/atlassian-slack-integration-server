package com.atlassian.plugins.slack.api.events;

import com.atlassian.analytics.api.annotations.EventName;
import com.atlassian.plugins.slack.analytics.AnalyticsContext;

public class OauthFlowEvent extends BaseAnalyticEvent {
    public enum Status {
        STARTED, SUCCEEDED, FAILED;
    }

    private final Status status;

    public OauthFlowEvent(final AnalyticsContext context, final Status status) {
        super(context);
        this.status = status;
    }

    @EventName
    public String getName() {
        return "notifications.slack.oauth." + status.name().toLowerCase();
    }
}
