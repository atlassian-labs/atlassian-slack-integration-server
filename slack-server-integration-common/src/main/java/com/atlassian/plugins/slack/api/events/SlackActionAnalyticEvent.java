package com.atlassian.plugins.slack.api.events;

import com.atlassian.analytics.api.annotations.EventName;
import com.atlassian.plugins.slack.analytics.AnalyticsContext;
import lombok.Getter;

@EventName("notifications.slack.inbound.action")
public class SlackActionAnalyticEvent extends BaseAnalyticEvent {
    // this property has limited and known possible values set, so there is no need to hash it
    // if unknown value will be used as sub-command it will be removed from the payload by product analytics system
    @Getter
    private final String type;

    public SlackActionAnalyticEvent(final AnalyticsContext context,
                                    final String type) {
        super(context);
        this.type = type;
    }
}
