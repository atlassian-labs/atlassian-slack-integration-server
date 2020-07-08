package com.atlassian.jira.plugins.slack.model.event;

import com.atlassian.jira.plugins.slack.model.analytics.AbstractProjectChannelEvent;
import com.atlassian.jira.plugins.slack.model.analytics.ProjectChannelLinkedEvent;
import com.atlassian.jira.plugins.slack.model.analytics.ProjectChannelUnlinkedEvent;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugins.slack.analytics.AnalyticsContext;
import lombok.Builder;
import lombok.Value;

/**
 * This is the default implementation of {@link com.atlassian.jira.plugins.slack.model.event.ConfigurationEvent}
 */
@Value
@Builder
public class ProjectMappingConfigurationEvent implements ConfigurationEvent {
    ConfigurationEventType eventType;
    long projectId;
    String projectKey;
    String projectName;
    String teamId;
    String channelId;
    ApplicationUser user;
    AnalyticsContext context;

    public AbstractProjectChannelEvent getAnalyticsEvent() {
        String userKey = user != null ? user.getKey() : null;
        if (eventType == ConfigurationEventType.CHANNEL_LINKED) {
            return new ProjectChannelLinkedEvent(context, channelId, userKey, projectId);
        } else if (eventType == ConfigurationEventType.CHANNEL_UNLINKED) {
            return new ProjectChannelUnlinkedEvent(context, channelId, userKey, projectId);
        } else {
            throw new IllegalArgumentException("Unknown event type: " + eventType);
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitProjectMappingConfigurationEvent(this);
    }
}
