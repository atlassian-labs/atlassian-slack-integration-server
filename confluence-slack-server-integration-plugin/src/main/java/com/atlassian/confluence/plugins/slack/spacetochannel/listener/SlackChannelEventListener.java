package com.atlassian.confluence.plugins.slack.spacetochannel.listener;

import com.atlassian.plugins.slack.util.AsyncExecutor;
import com.atlassian.confluence.plugins.slack.spacetochannel.service.SlackSpaceToChannelService;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugins.slack.api.events.SlackConversationsLoadedEvent;
import com.atlassian.plugins.slack.api.webhooks.ChannelArchiveSlackEvent;
import com.atlassian.plugins.slack.api.webhooks.ChannelDeletedSlackEvent;
import com.atlassian.plugins.slack.api.webhooks.ChannelUnarchiveSlackEvent;
import com.atlassian.plugins.slack.settings.SlackSettingService;
import com.atlassian.plugins.slack.util.AutoSubscribingEventListener;
import com.github.seratch.jslack.api.model.Conversation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SlackChannelEventListener extends AutoSubscribingEventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(SlackChannelEventListener.class);

    private final SlackSettingService settingService;
    private final SlackSpaceToChannelService spaceToChannelService;
    private final AsyncExecutor asyncExecutor;

    @Autowired
    public SlackChannelEventListener(final EventPublisher eventPublisher,
                                     final SlackSettingService settingService,
                                     final SlackSpaceToChannelService spaceToChannelService,
                                     final AsyncExecutor asyncExecutor) {
        super(eventPublisher);
        this.settingService = settingService;
        this.spaceToChannelService = spaceToChannelService;
        this.asyncExecutor = asyncExecutor;
    }

    @EventListener
    public void onChannelArchivedEvent(final ChannelArchiveSlackEvent event) {
        final String channelId = event.getChannel();
        asyncExecutor.run(() -> settingService.muteChannel(channelId));
    }

    @EventListener
    public void onChannelUnarchivedEvent(final ChannelUnarchiveSlackEvent event) {
        final String channelId = event.getChannel();
        asyncExecutor.run(() -> settingService.unmuteChannel(channelId));
    }

    @EventListener
    public void onChannelDeletedEvent(final ChannelDeletedSlackEvent event) {
        final String channelId = event.getChannel();
        asyncExecutor.run(() -> {
            LOGGER.debug("Removing notification mapping for channel {} because the channel was deleted", channelId);
            spaceToChannelService.removeNotificationsForChannel(channelId);
            settingService.unmuteChannel(channelId);
        });
    }

    @EventListener
    public void onConversationsLoadedEvent(final SlackConversationsLoadedEvent event) {
        // unmute channel if they are returned in the list of active channels;
        // just in case we missed 'channel_unarchive' event
        asyncExecutor.run(() -> {
            final List<String> mutedChannelIds = settingService.getMutedChannelIds();
            if (!mutedChannelIds.isEmpty()) {
                Set<String> activeConversationIds = event.getConversations().stream()
                        .map(Conversation::getId)
                        .collect(Collectors.toSet());
                mutedChannelIds.stream()
                        .filter(activeConversationIds::contains)
                        .forEach(settingService::unmuteChannel);
            }
        });
    }
}
