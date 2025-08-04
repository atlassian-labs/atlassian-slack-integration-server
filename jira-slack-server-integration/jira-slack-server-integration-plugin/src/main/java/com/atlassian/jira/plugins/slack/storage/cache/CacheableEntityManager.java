package com.atlassian.jira.plugins.slack.storage.cache;

import jakarta.annotation.Nonnull;

import java.util.Optional;

/**
 * Manages a Cacheable Entity
 */
public interface CacheableEntityManager<K, E extends CacheableEntity<K>> {
    /**
     * Get all the entity keys stored in cache
     *
     * @return all the entity keys stored in cache
     */
    @Nonnull
    Iterable<K> getKeys();

    /**
     * Get all the entities of type E from cache
     *
     * @return all the entities of type &lt;E&gt; for the given issue id
     */
    @Nonnull
    Iterable<E> getAll();

    /**
     * Get the stored entity with the given key from cache
     *
     * @param key the key for the stored entity
     * @return the stored entity with the given key from cache, or None if not found
     */
    @Nonnull
    Optional<E> get(@Nonnull K key);

    /**
     * Store the given entity in cache
     *
     * @param key    the entity key
     * @param entity the entity to store in cache
     */
    void put(@Nonnull K key, @Nonnull Optional<E> entity);

    /**
     * Delete the entity with the given key from cache
     *
     * @param key the entity key
     */
    void delete(@Nonnull K key);

    /**
     * Delete all entities in the cache
     */
    void deleteAll();
}
