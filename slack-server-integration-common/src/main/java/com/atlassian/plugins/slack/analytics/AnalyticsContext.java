package com.atlassian.plugins.slack.analytics;

import com.atlassian.plugins.slack.api.SlackUser;
import lombok.Value;

/**
 * Current user context to be passed to analytic event.
 * All fields are nullable for performance reasons.
 */
@Value
public class AnalyticsContext {
    String userKey;
    String teamId;
    String slackUserId;

    public static AnalyticsContext fromSlackUser(final SlackUser slackUser) {
        String userKey = null;
        String slackTeamId = null;
        String slackUserId = null;
        if (slackUser != null) {
            userKey = slackUser.getUserKey();
            slackTeamId = slackUser.getSlackTeamId();
            slackUserId = slackUser.getSlackUserId();
        }

        return new AnalyticsContext(userKey, slackTeamId, slackUserId);
    }
}
