package com.atlassian.confluence.plugins.slack.spacetochannel.notifications;

import com.atlassian.confluence.plugins.slack.spacetochannel.model.ChannelContext;
import com.atlassian.plugins.slack.api.notification.ChannelToNotify;
import com.atlassian.plugins.slack.api.notification.NotificationType;
import com.atlassian.plugins.slack.api.notification.SlackNotificationContext;

import java.util.Collections;
import java.util.List;

public class ChannelNotificationContext<T extends ChannelContext> implements SlackNotificationContext<T> {
    @Override
    public List<ChannelToNotify> getChannels(final T event, final NotificationType notificationType) {
        final String teamId = event.getTeamId();
        final String channelId = event.getChannelId();
        final String threadTs = event.getThreadTs();
        final ChannelToNotify channelToNotify = new ChannelToNotify(teamId, channelId, threadTs, false);
        return Collections.singletonList(channelToNotify);
    }
}
