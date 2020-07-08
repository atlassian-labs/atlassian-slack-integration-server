package com.atlassian.jira.plugins.slack.service.notification.impl;

import com.atlassian.jira.plugins.slack.model.SlackNotification;
import com.atlassian.jira.plugins.slack.model.event.PluginEvent;
import com.atlassian.jira.plugins.slack.service.notification.EventRenderer;
import com.atlassian.jira.plugins.slack.service.notification.NotificationInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component("eventRendererDispatcher")
public class EventRendererDispatcher implements EventRenderer {
    private final List<EventRenderer> eventRenderers;

    @Autowired
    public EventRendererDispatcher(final List<EventRenderer> eventRenderers) {
        this.eventRenderers = eventRenderers;
    }

    @Override
    public List<SlackNotification> render(PluginEvent pluginEvent, List<NotificationInfo> notificationInfos) {
        for (EventRenderer eventRenderer : eventRenderers) {
            if (eventRenderer.canRender(pluginEvent)) {
                return eventRenderer.render(pluginEvent, notificationInfos);
            }
        }
        return Collections.emptyList();
    }

    @Override
    public boolean canRender(PluginEvent pluginEvent) {
        return true;
    }
}
