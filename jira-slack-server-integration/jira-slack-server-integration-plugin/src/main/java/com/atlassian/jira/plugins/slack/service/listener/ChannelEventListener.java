package com.atlassian.jira.plugins.slack.service.listener;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.plugins.slack.manager.ProjectConfigurationManager;
import com.atlassian.jira.plugins.slack.model.ProjectConfiguration;
import com.atlassian.plugins.slack.api.events.SlackConversationsLoadedEvent;
import com.atlassian.plugins.slack.api.webhooks.ChannelArchiveSlackEvent;
import com.atlassian.plugins.slack.api.webhooks.ChannelDeletedSlackEvent;
import com.atlassian.plugins.slack.api.webhooks.ChannelUnarchiveSlackEvent;
import com.atlassian.plugins.slack.util.AutoSubscribingEventListener;
import com.github.seratch.jslack.api.model.Conversation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ChannelEventListener extends AutoSubscribingEventListener {
    private final ProjectConfigurationManager projectConfigurationManager;

    @Autowired
    public ChannelEventListener(final EventPublisher eventPublisher,
                                final ProjectConfigurationManager projectConfigurationManager) {
        super(eventPublisher);
        this.projectConfigurationManager = projectConfigurationManager;
    }

    @EventListener
    public void onChannelArchivedEvent(final ChannelArchiveSlackEvent event) {
        String channelId = event.getChannel();
        projectConfigurationManager.muteProjectConfigurationsByChannelId(channelId);
    }

    @EventListener
    public void onChannelUnarchivedEvent(final ChannelUnarchiveSlackEvent event) {
        String channelId = event.getChannel();
        projectConfigurationManager.unmuteProjectConfigurationsByChannelId(channelId);
    }

    @EventListener
    public void onChannelDeletedEvent(final ChannelDeletedSlackEvent event) {
        String channelId = event.getChannel();
        projectConfigurationManager.deleteProjectConfigurationsByChannelId(channelId);
    }

    @EventListener
    public void onConversationsLoadedEvent(final SlackConversationsLoadedEvent event) {
        // unmute channel if they are returned in the list of active channels;
        // just in case we missed 'channel_unarchive' event
        List<ProjectConfiguration> mutedConfigs = projectConfigurationManager.getMutedProjectConfigurations();
        if (!mutedConfigs.isEmpty()) {
            Set<String> activeConversationIds = event.getConversations().stream()
                    .map(Conversation::getId)
                    .collect(Collectors.toSet());
            mutedConfigs.stream()
                    .map(ProjectConfiguration::getChannelId)
                    .filter(activeConversationIds::contains)
                    .forEach(projectConfigurationManager::unmuteProjectConfigurationsByChannelId);
        }
    }
}
