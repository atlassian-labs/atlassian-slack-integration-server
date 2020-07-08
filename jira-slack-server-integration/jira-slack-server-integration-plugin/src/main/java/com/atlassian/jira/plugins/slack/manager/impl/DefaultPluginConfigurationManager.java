package com.atlassian.jira.plugins.slack.manager.impl;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.plugins.slack.manager.PluginConfigurationManager;
import com.atlassian.jira.plugins.slack.model.analytics.IssuePanelHiddenEvent;
import com.atlassian.jira.plugins.slack.model.analytics.IssuePanelShownEvent;
import com.atlassian.jira.plugins.slack.model.event.AutoConvertEvent;
import com.atlassian.jira.plugins.slack.model.event.AutoConvertEvent.Type;
import com.atlassian.plugins.slack.analytics.AnalyticsContextProvider;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class DefaultPluginConfigurationManager implements PluginConfigurationManager {
    static final String PLUGIN_STORAGE_KEY = "com.atlassian.slack";
    final static String GLOBAL_AUTOCONVERT_ENABLED = "global.autoconvert.enabled";
    final static String ISSUE_PANEL_HIDDEN = "issue.panel.hidden";
    final static String ISSUE_PREVIEW_IN_GUEST_CHANNELS_ENABLED = "issue.preview.in.guest.channels.enabled";

    private final AnalyticsContextProvider analyticsContextProvider;
    private final PluginSettingsFactory pluginSettings;
    private final EventPublisher eventPublisher;

    @Override
    public boolean isGlobalAutoConvertEnabled() {
        Object value = getSettings().get(GLOBAL_AUTOCONVERT_ENABLED);
        return value == null || Boolean.valueOf((String) value);
    }

    @Override
    public void setGlobalAutoConvertEnabled(boolean enabled) {
        getSettings().put(GLOBAL_AUTOCONVERT_ENABLED, Boolean.toString(enabled));
        eventPublisher.publish(new AutoConvertEvent(analyticsContextProvider.current(), Type.GLOBAL, enabled));
    }

    @Override
    public boolean isIssuePanelHidden() {
        Object value = getSettings().get(ISSUE_PANEL_HIDDEN);
        return value != null && Boolean.valueOf((String) value);
    }

    @Override
    public void setIssuePanelHidden(boolean hide) {
        getSettings().put(ISSUE_PANEL_HIDDEN, Boolean.toString(hide));
        if (hide) {
            eventPublisher.publish(new IssuePanelHiddenEvent(analyticsContextProvider.current(), 0));
        } else {
            eventPublisher.publish(new IssuePanelShownEvent(analyticsContextProvider.current(), 0));
        }
    }

    @Override
    public boolean isIssuePreviewForGuestChannelsEnabled() {
        Object value = getSettings().get(ISSUE_PREVIEW_IN_GUEST_CHANNELS_ENABLED);
        return value != null && Boolean.valueOf((String) value);
    }

    @Override
    public void setIssuePreviewForGuestChannelsEnabled(boolean value) {
        getSettings().put(ISSUE_PREVIEW_IN_GUEST_CHANNELS_ENABLED, Boolean.toString(value));
        eventPublisher.publish(new AutoConvertEvent(analyticsContextProvider.current(), Type.GUESTS, value));
    }

    @Override
    public PluginSettings getSettings() {
        return pluginSettings.createSettingsForKey(PLUGIN_STORAGE_KEY);
    }
}
