package com.atlassian.jira.plugins.slack.model.event;

import com.atlassian.jira.plugins.slack.model.analytics.AbstractProjectChannelEvent;

/**
 * This interface represents a configuration event, typically performed by an administrator.
 */
public interface ConfigurationEvent extends PluginEvent {
    interface Visitor<T> {
        T visitDedicatedChannelLinkedEvent(DedicatedChannelLinkedEvent event);

        T visitDedicatedChannelUnlinkedEvent(DedicatedChannelUnlinkedEvent event);

        T visitProjectMappingConfigurationEvent(ProjectMappingConfigurationEvent event);

        T visitUnauthorizedUnfurlConfigurationEvent(UnauthorizedUnfurlEvent event);
    }

    enum ConfigurationEventType {
        CHANNEL_LINKED,
        DEDICATED_CHANNEL_LINKED,
        CHANNEL_UNLINKED,
        DEDICATED_CHANNEL_UNLINKED,
        UNAUTHORIZED_UNFURL
    }

    ConfigurationEventType getEventType();

    long getProjectId();

    String getTeamId();

    String getChannelId();

    AbstractProjectChannelEvent getAnalyticsEvent();

    <T> T accept(Visitor<T> visitor);
}
