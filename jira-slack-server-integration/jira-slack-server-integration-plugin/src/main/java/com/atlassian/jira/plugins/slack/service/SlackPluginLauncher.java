package com.atlassian.jira.plugins.slack.service;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.plugins.slack.model.ao.ProjectConfigurationAO;
import com.atlassian.jira.plugins.slack.model.event.PluginStartedEvent;
import com.atlassian.jira.plugins.slack.util.PluginConstants;
import com.atlassian.plugin.event.events.PluginEnabledEvent;
import com.atlassian.plugins.slack.util.AutoSubscribingEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SlackPluginLauncher extends AutoSubscribingEventListener {
    private static final Logger log = LoggerFactory.getLogger(SlackPluginLauncher.class);

    private final ActiveObjects ao;

    @Autowired
    public SlackPluginLauncher(final ActiveObjects ao, final EventPublisher eventPublisher) {
        super(eventPublisher);
        this.ao = ao;
    }

    @EventListener
    public void onPluginEnabled(PluginEnabledEvent event) {
        if (PluginConstants.PLUGIN_KEY.equals(event.getPlugin().getKey())) {
            log.debug("Slack Plugin ready to initialize");

            //hit DB to check if everything is right
            ao.count(ProjectConfigurationAO.class);

            log.info("Slack Plugin launched successfully");

            eventPublisher.publish(new PluginStartedEvent());
        }
    }
}
