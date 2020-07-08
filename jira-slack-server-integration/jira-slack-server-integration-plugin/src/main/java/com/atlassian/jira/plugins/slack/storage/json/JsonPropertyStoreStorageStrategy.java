package com.atlassian.jira.plugins.slack.storage.json;

import com.atlassian.jira.plugins.slack.storage.EntityStorageStrategy;
import com.atlassian.jira.plugins.slack.storage.StorableEntity;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Entity Store provider that uses Jira's JSON storage facility
 */
public abstract class JsonPropertyStoreStorageStrategy<E extends StorableEntity<String>> implements EntityStorageStrategy<E> {
    private static final Logger log = LoggerFactory.getLogger(JsonPropertyStoreStorageStrategy.class);

    private final JsonStore<E> jsonStore;

    protected JsonPropertyStoreStorageStrategy(JsonStore<E> jsonStore) {
        this.jsonStore = checkNotNull(jsonStore);
    }

    /**
     * Returns all E's stored against the given issue id.
     *
     * @param issueId the issue id to look up
     * @return all E's stored against the given issue id
     */
    @Override
    @Nonnull
    public List<E> getAll(final long issueId) {
        Map<String, Optional<E>> entities = jsonStore.getAllForIssue(issueId);

        log.debug("Getting all enities of type '{}' for issueId={}: {}", getEntityType(), issueId, entities);

        if (entities == null || entities.isEmpty()) {
            log.debug("No entity of type '{}' found for issueId={}", getEntityType(), issueId);
            return ImmutableList.of();
        }

        return ImmutableList.copyOf(entities.values().stream()
                .filter(input -> input != null && input.isPresent())
                .map(input -> input.get())
                .collect(Collectors.toList()));
    }

    protected abstract Class<E> getEntityType();

    /**
     * Returns the E stored against the given issue id and entity key
     *
     * @param issueId   the issue id to look up
     * @param entityKey the entity storage key
     * @return an E or None
     */
    @Override
    @Nonnull
    public Optional<E> get(final long issueId, final String entityKey) {
        final Optional<E> entity = jsonStore.get(issueId, entityKey);

        log.debug("Getting entity of type '{}' for issueId={} with key={}: {}",
                getEntityType(), issueId, entityKey, entity.isPresent() ? entity.get() : "None");

        return entity;
    }

    /**
     * Stores the entity with the given key against the given issue id.
     *
     * @param issueId   the issue id
     * @param entityKey the entity key
     * @param entity    the entity to store
     */
    @Override
    public void put(final long issueId, @Nonnull final String entityKey, @Nonnull final Optional<E> entity) {
        if (!entity.isPresent()) {
            log.debug("Nothing to store for issueId={} and entityKey={} and entityType={}",
                    issueId, entityKey, getEntityType());
            return;
        }

        log.debug("Storing entity of type '{}' for issueId={} and entityKey={}: {}",
                getEntityType(), issueId, entityKey, entity);

        jsonStore.put(issueId, entityKey, entity);
    }

    /**
     * Delete the entity by issueId and entityKey
     */
    @Override
    public void delete(final long issueId, final String entityKey) {
        log.debug("Removing entity of type '{}' for issueId={} and entityKey={}",
                getEntityType(), issueId, entityKey);

        jsonStore.delete(issueId, entityKey);
    }

    @Override
    public List<E> getAll() {
        return jsonStore.getAll();
    }

    public List<E> findByPredicate(Predicate<E> criteria) {
        return jsonStore.findByPredicate(criteria);
    }

    @Override
    public void deleteAllByPropertyKey(String propertyKey) {
        jsonStore.deleteAllForKey(propertyKey);
    }
}
