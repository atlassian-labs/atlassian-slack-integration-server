package com.atlassian.jira.plugins.slack.service;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.plugins.slack.model.ao.ProjectConfigurationAO;
import com.atlassian.jira.plugins.slack.model.event.PluginStartedEvent;
import com.atlassian.jira.plugins.slack.util.PluginConstants;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.event.events.PluginEnabledEvent;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SlackPluginLauncherTest {
    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private ActiveObjects ao;

    @Mock
    private PluginEnabledEvent pluginEnabledEvent;
    @Mock
    private Plugin plugin;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private SlackPluginLauncher target;

    @Test
    public void onPluginEnabled() {
        when(pluginEnabledEvent.getPlugin()).thenReturn(plugin);
        when(plugin.getKey()).thenReturn(PluginConstants.PLUGIN_KEY);

        target.onPluginEnabled(pluginEnabledEvent);

        verify(ao).count(ProjectConfigurationAO.class);
        verify(eventPublisher).publish(isA(PluginStartedEvent.class));
    }
}
