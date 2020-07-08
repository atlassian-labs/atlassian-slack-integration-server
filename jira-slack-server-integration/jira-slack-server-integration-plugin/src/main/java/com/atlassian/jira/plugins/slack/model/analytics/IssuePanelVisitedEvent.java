package com.atlassian.jira.plugins.slack.model.analytics;

import com.atlassian.analytics.api.annotations.EventName;
import com.atlassian.plugins.slack.analytics.AnalyticsContext;
import com.atlassian.plugins.slack.api.events.BaseAnalyticEvent;
import lombok.Getter;

@EventName("jira.slack.integration.mentions.page.visited")
public class IssuePanelVisitedEvent extends BaseAnalyticEvent {
    @Getter
    private long mentionsCount;

    public IssuePanelVisitedEvent(final AnalyticsContext context,
                                  final long mentionsCount) {
        super(context);
        this.mentionsCount = mentionsCount;
    }
}
