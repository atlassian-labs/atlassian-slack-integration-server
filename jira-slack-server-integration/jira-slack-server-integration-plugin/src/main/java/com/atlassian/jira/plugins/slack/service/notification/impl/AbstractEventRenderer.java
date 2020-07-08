package com.atlassian.jira.plugins.slack.service.notification.impl;

import com.atlassian.jira.plugins.slack.model.SlackNotification;
import com.atlassian.jira.plugins.slack.model.event.PluginEvent;
import com.atlassian.jira.plugins.slack.service.notification.EventRenderer;
import com.atlassian.jira.plugins.slack.service.notification.NotificationInfo;

import java.util.Collections;
import java.util.List;

public abstract class AbstractEventRenderer<T extends PluginEvent> implements EventRenderer {
    @Override
    public List<SlackNotification> render(PluginEvent pluginEvent, List<NotificationInfo> notificationInfos) {

        if (!canRender(pluginEvent)) {
            return Collections.emptyList();
        }

        //noinspection unchecked
        return doRender((T) pluginEvent, notificationInfos);
    }

    protected abstract List<SlackNotification> doRender(T pluginEvent, List<NotificationInfo> notificationInfos);
}
