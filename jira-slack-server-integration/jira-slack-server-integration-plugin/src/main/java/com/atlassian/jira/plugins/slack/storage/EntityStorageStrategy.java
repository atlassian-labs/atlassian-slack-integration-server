package com.atlassian.jira.plugins.slack.storage;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

/**
 * Entity storage strategy
 */
public interface EntityStorageStrategy<E extends StorableEntity> {
    /**
     * Get all the entities of this type for the given issue id
     *
     * @param issueId the issue id
     * @return all the entities of type &lt;E&gt; for the given issue id
     */
    @Nonnull
    List<E> getAll(long issueId);

    /**
     * Get the stored entity with the given key for the given issue
     *
     * @param issueId   the issue id
     * @param entityKey the key for the stored entity
     * @return the entity with the given key and issue id, or None if not found
     */
    @Nonnull
    Optional<E> get(long issueId, @Nonnull String entityKey);

    /**
     * Store the given entity against the given key and issue id
     *
     * @param issueId   the issue id
     * @param entityKey the storage key to store the entity against
     * @param entity    the storage entity to store
     */
    void put(long issueId, @Nonnull String entityKey, @Nonnull Optional<E> entity);

    /**
     * Delete the entity with the given key and issue id
     *
     * @param issueId   the issue id
     * @param entityKey the entity key
     */
    void delete(long issueId, @Nonnull String entityKey);

    /**
     * @return all the entities for all issues
     */
    List<E> getAll();

    /**
     * Delete all the entities for the given key, for all issues
     *
     * @param propertyKey the key
     */
    void deleteAllByPropertyKey(String propertyKey);
}
