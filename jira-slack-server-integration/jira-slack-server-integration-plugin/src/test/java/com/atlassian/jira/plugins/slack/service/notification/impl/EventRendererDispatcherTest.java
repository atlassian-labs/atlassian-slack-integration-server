package com.atlassian.jira.plugins.slack.service.notification.impl;

import com.atlassian.jira.plugins.slack.model.SlackNotification;
import com.atlassian.jira.plugins.slack.model.event.PluginEvent;
import com.atlassian.jira.plugins.slack.service.notification.EventRenderer;
import com.atlassian.jira.plugins.slack.service.notification.NotificationInfo;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EventRendererDispatcherTest {
    @Mock
    private EventRenderer eventRenderer;
    @Mock
    private EventRenderer eventRendererNotCompatible;
    @Mock
    private EventRenderer eventRendererComingLate;
    @Mock
    private PluginEvent pluginEvent;
    @Mock
    private NotificationInfo notificationInfo;
    @Mock
    private SlackNotification slackNotification;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Test
    public void render_useFirstAvailableRender() {
        when(eventRendererNotCompatible.canRender(pluginEvent)).thenReturn(false);
        when(eventRenderer.canRender(pluginEvent)).thenReturn(true);
        when(eventRenderer.render(pluginEvent, singletonList(notificationInfo))).thenReturn(singletonList(slackNotification));

        List<EventRenderer> eventRenderers = Arrays.asList(eventRendererNotCompatible, eventRenderer, eventRendererComingLate);
        EventRendererDispatcher target = new EventRendererDispatcher(eventRenderers);

        assertThat(target.render(pluginEvent, singletonList(notificationInfo)), contains(slackNotification));
        verify(eventRendererComingLate, never()).canRender(any());
    }

    @Test
    public void render_emptyIfNoRendersAreFound() {
        when(eventRenderer.canRender(pluginEvent)).thenReturn(false);

        List<EventRenderer> eventRenderers = singletonList(eventRenderer);
        EventRendererDispatcher target = new EventRendererDispatcher(eventRenderers);

        assertThat(target.render(pluginEvent, singletonList(notificationInfo)), empty());
    }

    @Test
    public void canRender() {
        assertThat(new EventRendererDispatcher(Collections.emptyList()).canRender(null), is(true));
    }
}
