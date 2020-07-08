package com.atlassian.jira.plugins.slack.service.notification;

import com.atlassian.jira.plugins.slack.model.SlackNotification;
import com.atlassian.jira.plugins.slack.model.event.PluginEvent;

import java.util.List;

/**
 * Renders events message for the notifications
 */
public interface EventRenderer {
    /**
     * Renders event for the given channels
     */
    List<SlackNotification> render(PluginEvent pluginEvent, List<NotificationInfo> slackMessageEvents);

    /**
     * Determines if a pluginEvent can be rendered by this renderer
     *
     * @param pluginEvent plugin event
     * @return true if the event can be rendered, false otherwise
     */
    boolean canRender(PluginEvent pluginEvent);
}
