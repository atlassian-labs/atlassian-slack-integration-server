package com.atlassian.confluence.plugins.slack.spacetochannel.ao;

import net.java.ao.Entity;
import net.java.ao.Preload;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.Table;

@Preload("*")
@Table(value = "EntitiesToChannels")
public interface AOEntityToChannelMapping extends Entity {
    String CHANNEL_ID_COLUMN = "CHANNEL_ID";

    /**
     * @return the Confluence space key that the entity relates to.
     */
    @Indexed
    String getEntityKey();

    void setEntityKey(final String key);

    /**
     * @return the user key that created or updated this configuration.
     */
    String getOwner();

    void setOwner(final String userKey);

    /**
     * @return the Slack team ID that the entity relates to.
     */
    @Indexed
    String getTeamId();

    void setTeamId(final String channelId);

    /**
     * @return the Slack channel ID that the entity relates to.
     */
    @Indexed
    String getChannelId();

    void setChannelId(final String channelId);

    /**
     * @return the Confluence message type the entity relates to.
     */
    String getMessageTypeKey();

    /**
     * Update the message type.
     *
     * @param messageTypeKey the new message type.
     */
    void setMessageTypeKey(final String messageTypeKey);
}
