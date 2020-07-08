package com.atlassian.plugins.slack.api.events;

import com.atlassian.analytics.api.annotations.EventName;
import com.atlassian.plugins.slack.analytics.AnalyticsContext;

@EventName("notifications.slack.inbound.bot.mention")
public class SlackBotMentionAnalyticEvent extends BaseAnalyticEvent {
    public SlackBotMentionAnalyticEvent(final AnalyticsContext context) {
        super(context);
    }
}
