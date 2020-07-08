package com.atlassian.confluence.plugins.slack.spacetochannel.listener;

import com.atlassian.confluence.event.events.space.SpaceRemoveEvent;
import com.atlassian.confluence.plugins.slack.spacetochannel.service.SlackSpaceToChannelService;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugins.slack.util.AutoSubscribingEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SlackSpaceEventListener extends AutoSubscribingEventListener {
    private final static Logger log = LoggerFactory.getLogger(SlackSpaceEventListener.class);

    private final SlackSpaceToChannelService spaceToChannelService;

    @Autowired
    public SlackSpaceEventListener(final EventPublisher eventPublisher,
                                   final SlackSpaceToChannelService spaceToChannelService) {
        super(eventPublisher);
        this.spaceToChannelService = spaceToChannelService;
    }

    @EventListener
    public void spaceRemovedEvent(final SpaceRemoveEvent spaceRemoveEvent) {
        final String spaceKey = spaceRemoveEvent.getSpace().getKey();
        log.debug("Removing channel notifications for deleted space {}", spaceKey);
        int notificationsRemoved = spaceToChannelService.removeNotificationsForSpace(spaceKey);
        log.debug("{} channel notifications removed for deleted space {}", notificationsRemoved, spaceKey);
    }
}
