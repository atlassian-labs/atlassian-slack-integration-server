package com.atlassian.jira.plugins.slack.storage.json;

import com.atlassian.jira.entity.property.JsonEntityPropertyManager;
import com.atlassian.jira.plugins.slack.storage.StorableEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Factory for creating {@link com.atlassian.jira.plugins.slack.storage.json.JsonStore} instances.
 */
@Service
public class JsonStoreFactory {
    private final JsonEntityPropertyManager jsonEntityPropertyManager;

    @Autowired
    public JsonStoreFactory(JsonEntityPropertyManager jsonEntityPropertyManager) {
        this.jsonEntityPropertyManager = checkNotNull(jsonEntityPropertyManager);
    }

    public <T extends StorableEntity<String>> JsonStore<T> getJsonStore(String entityName, Class<T> entityType) {
        return new JsonStore<T>(jsonEntityPropertyManager,
                entityName,
                entityType);
    }
}
