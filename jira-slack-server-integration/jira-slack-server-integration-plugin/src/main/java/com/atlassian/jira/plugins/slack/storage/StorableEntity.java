package com.atlassian.jira.plugins.slack.storage;

/**
 * A serializable and storable entity
 */
public interface StorableEntity<K> {
    K getKey();
}
