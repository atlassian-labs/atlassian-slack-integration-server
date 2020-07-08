package com.atlassian.confluence.plugins.slack.spacetochannel.events;

import com.atlassian.confluence.plugins.slack.spacetochannel.configuration.SlackChannelDefinition;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.user.ConfluenceUser;

/**
 * Fired when a space is linked to a channel.
 */
public class SpaceToChannelLinkedEvent extends SpaceToChannelConfigEvent {
    public SpaceToChannelLinkedEvent(final Space space, final SlackChannelDefinition channel, final ConfluenceUser user) {
        super(space, channel, user);
    }
}
