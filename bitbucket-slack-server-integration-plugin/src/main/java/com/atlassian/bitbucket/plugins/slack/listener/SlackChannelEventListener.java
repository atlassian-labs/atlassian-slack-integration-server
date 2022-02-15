package com.atlassian.bitbucket.plugins.slack.listener;

import com.atlassian.bitbucket.plugins.slack.notification.configuration.NotificationConfigurationService;
import com.atlassian.event.api.EventListener;
import com.atlassian.plugins.slack.api.ConversationKey;
import com.atlassian.plugins.slack.api.events.SlackConversationsLoadedEvent;
import com.atlassian.plugins.slack.api.webhooks.ChannelArchiveSlackEvent;
import com.atlassian.plugins.slack.api.webhooks.ChannelDeletedSlackEvent;
import com.atlassian.plugins.slack.api.webhooks.ChannelUnarchiveSlackEvent;
import com.atlassian.plugins.slack.settings.SlackSettingService;
import com.atlassian.plugins.slack.util.AsyncExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SlackChannelEventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(SlackChannelEventListener.class);

    private final SlackSettingService settingService;
    private final NotificationConfigurationService notificationService;
    private final AsyncExecutor asyncExecutor;

    @Autowired
    public SlackChannelEventListener(final SlackSettingService settingService,
                                     final NotificationConfigurationService notificationService,
                                     final AsyncExecutor asyncExecutor) {
        this.settingService = settingService;
        this.notificationService = notificationService;
        this.asyncExecutor = asyncExecutor;
    }

    @EventListener
    public void onChannelArchivedEvent(final ChannelArchiveSlackEvent event) {
        final String channelId = event.getChannel();
        final String teamId = event.getSlackEvent().getTeamId();
        asyncExecutor.run(() -> settingService.muteChannel(new ConversationKey(teamId, channelId)));
    }

    @EventListener
    public void onChannelUnarchivedEvent(final ChannelUnarchiveSlackEvent event) {
        final String channelId = event.getChannel();
        final String teamId = event.getSlackEvent().getTeamId();
        asyncExecutor.run(() -> settingService.unmuteChannel(new ConversationKey(teamId, channelId)));
    }

    @EventListener
    public void onChannelDeletedEvent(final ChannelDeletedSlackEvent event) {
        final String channelId = event.getChannel();
        final String teamId = event.getSlackEvent().getTeamId();
        asyncExecutor.run(() -> {
            LOGGER.debug("Removing notification mapping for channel {} because the channel was deleted", channelId);
            notificationService.removeNotificationsForChannel(new ConversationKey(teamId, channelId));
            settingService.unmuteChannel(new ConversationKey(teamId, channelId));
        });
    }

    @EventListener
    public void onConversationsLoadedEvent(final SlackConversationsLoadedEvent event) {
        // unmute channel if they are returned in the list of active channels;
        // just in case we missed 'channel_unarchive' event
        asyncExecutor.run(() -> {
            final List<ConversationKey> mutedChannelIds = settingService.getMutedChannelIds();
            if (!mutedChannelIds.isEmpty()) {
                Set<ConversationKey> activeConversationIds = event.getConversations().stream()
                        .map(c -> new ConversationKey(event.getTeamId(), c.getId()))
                        .collect(Collectors.toSet());
                mutedChannelIds.stream()
                        .filter(activeConversationIds::contains)
                        .forEach(settingService::unmuteChannel);
            }
        });
    }
}
