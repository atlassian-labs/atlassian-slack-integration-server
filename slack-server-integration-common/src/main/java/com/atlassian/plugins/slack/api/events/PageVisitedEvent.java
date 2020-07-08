package com.atlassian.plugins.slack.api.events;

import com.atlassian.analytics.api.annotations.EventName;
import com.atlassian.plugins.slack.analytics.AnalyticsContext;

public class PageVisitedEvent extends BaseAnalyticEvent {
    public enum CommonPage implements VisitablePage {
        GLOBAL_CONFIG("global.config"),
        PERSONAL_CONFIG("personal.config"),
        CONNECT_TEAM("connect.team"),
        EDIT_TEAM("edit.team"),
        OAUTH_SESSION("oauth.sessions");

        private final String suffix;

        CommonPage(final String suffix) {
            this.suffix = suffix;
        }

        @Override
        public String getSuffix() {
            return suffix;
        }
    }
    private final VisitablePage page;

    public PageVisitedEvent(final AnalyticsContext context, final VisitablePage page) {
        super(context);
        this.page = page;
    }

    @EventName
    public String getName() {
        return "notifications.slack.page.visited." + page.getSuffix();
    }
}
