package com.atlassian.jira.plugins.slack.dao.impl;

import com.atlassian.jira.plugins.slack.dao.DedicatedChannelDAO;
import com.atlassian.jira.plugins.slack.model.DedicatedChannel;
import com.atlassian.plugins.slack.api.ConversationKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class DefaultDedicatedChannelDao implements DedicatedChannelDAO {
    static final String DEDICATED_CHANNEL_PROPERTY_KEY = "slack.issue.dedicated.channel";

    private final DedicatedChannelStore dedicatedChannelStore;

    @Autowired
    public DefaultDedicatedChannelDao(final DedicatedChannelStore dedicatedChannelStore) {
        this.dedicatedChannelStore = dedicatedChannelStore;
    }

    @Override
    public void insertDedicatedChannel(final DedicatedChannel dedicatedChannel) {
        dedicatedChannelStore.put(dedicatedChannel.getIssueId(), DEDICATED_CHANNEL_PROPERTY_KEY, Optional.of(dedicatedChannel));
    }

    @Override
    public Optional<DedicatedChannel> getDedicatedChannel(final long issueId) {
        return dedicatedChannelStore.get(issueId, DEDICATED_CHANNEL_PROPERTY_KEY);
    }

    @Override
    public List<DedicatedChannel> findMappingsForChannel(final ConversationKey conversationKey) {
        return dedicatedChannelStore.getAllForChannel(conversationKey);
    }

    @Override
    public List<DedicatedChannel> findMappingsByTeamId(final String teamId) {
        return dedicatedChannelStore.getAllByTeamId(teamId);
    }

    @Override
    public void deleteDedicatedChannel(final long issueId) {
        dedicatedChannelStore.delete(issueId, DEDICATED_CHANNEL_PROPERTY_KEY);
    }
}
