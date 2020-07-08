package com.atlassian.jira.plugins.slack.model.analytics;

import com.atlassian.analytics.api.annotations.EventName;
import com.atlassian.plugins.slack.analytics.AnalyticsContext;
import com.atlassian.plugins.slack.api.events.BaseAnalyticEvent;
import lombok.Getter;

@EventName("jira.slack.integration.issuepanel.hidden")
public class IssuePanelHiddenEvent extends BaseAnalyticEvent {
    @Getter
    private long projectId;

    public IssuePanelHiddenEvent(final AnalyticsContext context,
                                 final long projectId) {
        super(context);
        this.projectId = projectId;
    }
}
