package com.atlassian.plugins.slack.api.events;

import com.atlassian.analytics.api.annotations.EventName;
import com.atlassian.plugins.slack.analytics.AnalyticsContext;
import com.atlassian.plugins.slack.util.DigestUtil;

/**
 * This event is fired when a request is received to start installing the Slack integration in Jira, and a 'src'
 * parameter is provided
 */
@EventName("notifications.slack.team.registration.page.permission.error")
public class SlackRegistrationPageHitByNonAdminUser extends BaseAnalyticEvent {
    private final String source;

    public SlackRegistrationPageHitByNonAdminUser(final AnalyticsContext context, final String source) {
        super(context);
        this.source = source;
    }

    // Jira explicitly disallows properties named 'source' on analytics events
    public long getSourceNameHash() {
        return DigestUtil.crc32(source);
    }
}
