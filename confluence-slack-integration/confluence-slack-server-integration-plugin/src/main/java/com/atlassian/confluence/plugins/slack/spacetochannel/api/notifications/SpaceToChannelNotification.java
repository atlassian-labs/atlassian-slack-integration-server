package com.atlassian.confluence.plugins.slack.spacetochannel.api.notifications;

import com.atlassian.confluence.spaces.Space;
import com.atlassian.plugins.slack.api.notification.SlackNotification;

import java.util.Optional;

public interface SpaceToChannelNotification<T extends ConfluenceSlackEvent> extends SlackNotification<T> {
    Optional<Space> getSpace(T event);
}
