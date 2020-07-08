package com.atlassian.jira.plugins.slack.model;

import com.atlassian.jira.plugins.slack.model.dto.DedicatedChannelDTO;
import com.atlassian.jira.plugins.slack.storage.StorableEntity;
import org.codehaus.jackson.map.annotate.JsonDeserialize;

@JsonDeserialize(as = DedicatedChannelDTO.class)
public interface DedicatedChannel extends StorableEntity<String> {
    long getIssueId();

    String getName();

    String getTeamId();

    String getChannelId();

    boolean isPrivateChannel();

    String getCreator();
}
