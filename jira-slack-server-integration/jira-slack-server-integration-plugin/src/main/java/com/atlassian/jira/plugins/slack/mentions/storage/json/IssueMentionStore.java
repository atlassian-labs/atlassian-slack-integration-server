package com.atlassian.jira.plugins.slack.mentions.storage.json;

import com.atlassian.jira.plugins.slack.model.mentions.IssueMention;
import com.atlassian.jira.plugins.slack.storage.json.JsonPropertyStoreStorageStrategy;
import com.atlassian.jira.plugins.slack.storage.json.JsonStoreFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;

/**
 * A store for Slack issue mentions
 */
@Service
public class IssueMentionStore extends JsonPropertyStoreStorageStrategy<IssueMention> {
    static final String SLACK_ISSUE_MENTIONS_ENTITY_NAME = "slack.integration.caches.issue-mentions";

    @Autowired
    public IssueMentionStore(@Nonnull final JsonStoreFactory jsonStoreFactory) {
        super(jsonStoreFactory.getJsonStore(SLACK_ISSUE_MENTIONS_ENTITY_NAME, IssueMention.class));
    }

    @Override
    protected Class<IssueMention> getEntityType() {
        return IssueMention.class;
    }
}
