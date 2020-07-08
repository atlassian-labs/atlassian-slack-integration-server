package com.atlassian.plugins.slack.api.events;

import com.atlassian.analytics.api.annotations.EventName;
import com.atlassian.plugins.slack.analytics.AnalyticsContext;

@EventName("notifications.slack.inbound.link.shared")
public class SlackLinkSharedAnalyticEvent extends BaseAnalyticEvent {
    public SlackLinkSharedAnalyticEvent(final AnalyticsContext context) {
        super(context);
    }
}
