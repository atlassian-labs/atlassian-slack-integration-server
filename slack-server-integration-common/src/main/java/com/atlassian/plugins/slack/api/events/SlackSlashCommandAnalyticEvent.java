package com.atlassian.plugins.slack.api.events;

import com.atlassian.analytics.api.annotations.EventName;
import com.atlassian.plugins.slack.analytics.AnalyticsContext;
import lombok.Getter;

@EventName("notifications.slack.inbound.slash.command")
public class SlackSlashCommandAnalyticEvent extends BaseAnalyticEvent {
    // this property has limited and known possible values set, so there is no need to hash it
    // if unknown value will be used as sub-command it will be removed from the payload by product analytics system
    @Getter
    private final String subCommand;

    public SlackSlashCommandAnalyticEvent(final AnalyticsContext context,
                                          final String subCommand) {
        super(context);
        this.subCommand = subCommand;
    }
}
