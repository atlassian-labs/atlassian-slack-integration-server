package com.atlassian.confluence.plugins.slack.spacetochannel.api.notifications;

import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.plugins.slack.api.notification.BaseSlackEvent;

/**
 * Base of all events that are handled by Slack plugin
 */
public interface ConfluenceSlackEvent extends BaseSlackEvent {
    Space getSpace();

    ConfluenceUser getUser();

    String getLink();
}
