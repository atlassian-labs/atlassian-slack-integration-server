package com.atlassian.jira.plugins.slack.dao;

import com.atlassian.jira.plugins.slack.model.DedicatedChannel;
import com.atlassian.plugins.slack.api.ConversationKey;

import java.util.List;
import java.util.Optional;

/**
 * This class manages the dedicated channel records in database.
 */
public interface DedicatedChannelDAO {
    /**
     * Inserts a new dedicated channel record.
     *
     * @param dedicatedChannel Dedicated channel
     */
    void insertDedicatedChannel(DedicatedChannel dedicatedChannel);

    /**
     * Returns the dedicated channel for the given issue if any
     *
     * @param issueId issue id
     * @return some dedicated channel or none
     */
    Optional<DedicatedChannel> getDedicatedChannel(long issueId);

    /**
     * Find the DedicatedChannel objects for a given channel name
     *
     * @param conversationKey the channel id
     * @return a iterable of dedicated channels
     */
    List<DedicatedChannel> findMappingsForChannel(ConversationKey conversationKey);

    /**
     * Find the DedicatedChannel objects assiciated with any channel in specified team.
     *
     * @param teamId team id
     * @return a iterable of dedicated channels
     */
    List<DedicatedChannel> findMappingsByTeamId(String teamId);

    /**
     * Remove a DedicatedChannel record
     *
     * @param issueId issue id
     */
    void deleteDedicatedChannel(long issueId);
}
