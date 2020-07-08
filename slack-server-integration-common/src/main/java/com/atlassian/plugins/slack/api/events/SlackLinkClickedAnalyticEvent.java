package com.atlassian.plugins.slack.api.events;

import com.atlassian.analytics.api.annotations.EventName;
import com.atlassian.plugins.slack.analytics.AnalyticsContext;
import lombok.Getter;

@EventName("notifications.slack.link.clicked")
public class SlackLinkClickedAnalyticEvent extends BaseAnalyticEvent {
    @Getter
    private final String type;

    public SlackLinkClickedAnalyticEvent(final AnalyticsContext context, final String type) {
        super(context);
        this.type = type;
    }
}
