package com.atlassian.jira.plugins.slack.manager.impl;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.plugins.slack.model.analytics.IssuePanelHiddenEvent;
import com.atlassian.jira.plugins.slack.model.analytics.IssuePanelShownEvent;
import com.atlassian.jira.plugins.slack.model.event.AutoConvertEvent;
import com.atlassian.plugins.slack.analytics.AnalyticsContextProvider;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static com.atlassian.jira.plugins.slack.manager.impl.DefaultPluginConfigurationManager.GLOBAL_AUTOCONVERT_ENABLED;
import static com.atlassian.jira.plugins.slack.manager.impl.DefaultPluginConfigurationManager.ISSUE_PANEL_HIDDEN;
import static com.atlassian.jira.plugins.slack.manager.impl.DefaultPluginConfigurationManager.ISSUE_PREVIEW_IN_GUEST_CHANNELS_ENABLED;
import static com.atlassian.jira.plugins.slack.manager.impl.DefaultPluginConfigurationManager.PLUGIN_STORAGE_KEY;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultPluginConfigurationManagerTest {
    @Mock
    private PluginSettingsFactory pluginSettingsFactory;
    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private AutoConvertEvent autoConvertEvent;
    @Mock
    private PluginSettings pluginSettings;
    @Mock
    AnalyticsContextProvider analyticsContextProvider;

    @Captor
    private ArgumentCaptor<AutoConvertEvent> autoConvertEventArgumentCaptor;
    @Captor
    private ArgumentCaptor<IssuePanelHiddenEvent> issuePanelHiddenEventArgumentCaptor;
    @Captor
    private ArgumentCaptor<IssuePanelShownEvent> issuePanelShownEventArgumentCaptor;

    @InjectMocks
    private DefaultPluginConfigurationManager target;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Test
    public void isGlobalAutoConvertEnabled() {
        when(pluginSettingsFactory.createSettingsForKey(PLUGIN_STORAGE_KEY)).thenReturn(pluginSettings);
        when(pluginSettings.get(GLOBAL_AUTOCONVERT_ENABLED)).thenReturn("true", "false", null);

        assertThat(target.isGlobalAutoConvertEnabled(), is(true));
        assertThat(target.isGlobalAutoConvertEnabled(), is(false));
        assertThat(target.isGlobalAutoConvertEnabled(), is(true));
    }

    @Test
    public void setGlobalAutoConvertEnabled() {
        when(pluginSettingsFactory.createSettingsForKey(PLUGIN_STORAGE_KEY)).thenReturn(pluginSettings);

        target.setGlobalAutoConvertEnabled(true);

        verify(pluginSettings).put(GLOBAL_AUTOCONVERT_ENABLED, "true");
        verify(eventPublisher).publish(autoConvertEventArgumentCaptor.capture());
        assertThat(autoConvertEventArgumentCaptor.getValue().getAnalyticEventName(), is("notifications.slack.autoconvert.global.enabled"));
    }

    @Test
    public void setGlobalAutoConvertDisabled() {
        when(pluginSettingsFactory.createSettingsForKey(PLUGIN_STORAGE_KEY)).thenReturn(pluginSettings);

        target.setGlobalAutoConvertEnabled(false);

        verify(pluginSettings).put(GLOBAL_AUTOCONVERT_ENABLED, "false");
        verify(eventPublisher).publish(autoConvertEventArgumentCaptor.capture());
        assertThat(autoConvertEventArgumentCaptor.getValue().getAnalyticEventName(), is("notifications.slack.autoconvert.global.disabled"));
    }

    @Test
    public void isIssuePanelHidden() {
        when(pluginSettingsFactory.createSettingsForKey(PLUGIN_STORAGE_KEY)).thenReturn(pluginSettings);
        when(pluginSettings.get(ISSUE_PANEL_HIDDEN)).thenReturn("true", "false", null);

        assertThat(target.isIssuePanelHidden(), is(true));
        assertThat(target.isIssuePanelHidden(), is(false));
        assertThat(target.isIssuePanelHidden(), is(false));
    }

    @Test
    public void setIssuePanelHiddenTrue() {
        when(pluginSettingsFactory.createSettingsForKey(PLUGIN_STORAGE_KEY)).thenReturn(pluginSettings);

        target.setIssuePanelHidden(true);

        verify(pluginSettings).put(ISSUE_PANEL_HIDDEN, "true");
        verify(eventPublisher).publish(issuePanelHiddenEventArgumentCaptor.capture());
        assertThat(issuePanelHiddenEventArgumentCaptor.getValue(), notNullValue());
    }

    @Test
    public void setIssuePanelHiddenFalse() {
        when(pluginSettingsFactory.createSettingsForKey(PLUGIN_STORAGE_KEY)).thenReturn(pluginSettings);

        target.setIssuePanelHidden(false);

        verify(pluginSettings).put(ISSUE_PANEL_HIDDEN, "false");
        verify(eventPublisher).publish(issuePanelShownEventArgumentCaptor.capture());
        assertThat(issuePanelShownEventArgumentCaptor.getValue(), notNullValue());
    }

    @Test
    public void isIssuePreviewForGuestChannelsEnabled() {
        when(pluginSettingsFactory.createSettingsForKey(PLUGIN_STORAGE_KEY)).thenReturn(pluginSettings);
        when(pluginSettings.get(ISSUE_PREVIEW_IN_GUEST_CHANNELS_ENABLED)).thenReturn("true", "false", null);

        assertThat(target.isIssuePreviewForGuestChannelsEnabled(), is(true));
        assertThat(target.isIssuePreviewForGuestChannelsEnabled(), is(false));
        assertThat(target.isIssuePreviewForGuestChannelsEnabled(), is(false));
    }

    @Test
    public void setIssuePreviewForGuestChannelsEnabled() {
        when(pluginSettingsFactory.createSettingsForKey(PLUGIN_STORAGE_KEY)).thenReturn(pluginSettings);

        target.setIssuePreviewForGuestChannelsEnabled(true);

        verify(pluginSettings).put(ISSUE_PREVIEW_IN_GUEST_CHANNELS_ENABLED, "true");
        verify(eventPublisher).publish(autoConvertEventArgumentCaptor.capture());
        assertThat(autoConvertEventArgumentCaptor.getValue().getAnalyticEventName(), is("notifications.slack.autoconvert.guests.enabled"));
    }

    @Test
    public void setIssuePreviewForGuestChannelsDisabled() {
        when(pluginSettingsFactory.createSettingsForKey(PLUGIN_STORAGE_KEY)).thenReturn(pluginSettings);

        target.setIssuePreviewForGuestChannelsEnabled(false);

        verify(pluginSettings).put(ISSUE_PREVIEW_IN_GUEST_CHANNELS_ENABLED, "false");
        verify(eventPublisher).publish(autoConvertEventArgumentCaptor.capture());
        assertThat(autoConvertEventArgumentCaptor.getValue().getAnalyticEventName(), is("notifications.slack.autoconvert.guests.disabled"));
    }
}
