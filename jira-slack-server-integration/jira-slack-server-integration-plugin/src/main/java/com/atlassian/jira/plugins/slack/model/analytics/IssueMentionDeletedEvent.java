package com.atlassian.jira.plugins.slack.model.analytics;

import com.atlassian.analytics.api.annotations.EventName;
import com.atlassian.plugins.slack.analytics.AnalyticsContext;
import com.atlassian.plugins.slack.api.events.BaseAnalyticEvent;

@EventName("jira.slack.integration.mentions.deleted")
public class IssueMentionDeletedEvent extends BaseAnalyticEvent {
    public IssueMentionDeletedEvent(final AnalyticsContext context) {
        super(context);
    }
}
