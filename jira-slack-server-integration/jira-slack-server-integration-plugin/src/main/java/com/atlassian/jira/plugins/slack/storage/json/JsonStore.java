package com.atlassian.jira.plugins.slack.storage.json;

import com.atlassian.jira.entity.property.EntityProperty;
import com.atlassian.jira.entity.property.JsonEntityPropertyManager;
import com.atlassian.jira.plugins.slack.storage.StorableEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptyList;

/**
 * A read-through cache that caches JSON objects keyed by issue. The cache is persisted in Jira by means of the {@link
 * JsonEntityPropertyManager}.
 */
public class JsonStore<T extends StorableEntity<String>> {
    private static final Logger log = LoggerFactory.getLogger(JsonStore.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final JsonEntityPropertyManager jsonEntityPropertyManager;
    private final String entityName;
    private final Class<T> storedClass;

    /**
     * Creates a new JsonStore.
     *
     * @param jsonEntityPropertyManager a JsonEntityPropertyManager
     * @param entityName                the entity name
     * @param storedClass               the type of object being stored as JSON
     * @throws IllegalArgumentException if {@code storedClass} can not be serialised
     */
    public JsonStore(@Nonnull final JsonEntityPropertyManager jsonEntityPropertyManager,
                     @Nonnull final String entityName,
                     @Nonnull final Class<T> storedClass) {
        this.jsonEntityPropertyManager = checkNotNull(jsonEntityPropertyManager, "jsonEntityPropertyManager is null");
        this.entityName = checkNotNull(entityName, "entityName is null");
        this.storedClass = checkNotNull(storedClass, "storedClass is null");
        if (!OBJECT_MAPPER.canSerialize(storedClass)) {
            throw new IllegalArgumentException("Can't serialise " + storedClass);
        }
    }

    /**
     * @param issueId the issue id to look up
     * @param key     the key of the property to retrieve
     * @return an Optional of T or None if no item was found
     */
    @Nonnull
    public Optional<T> get(final long issueId, final String key) {
        final EntityProperty json = jsonEntityPropertyManager.get(entityName, issueId, key);

        if (json == null) {
            return Optional.empty();
        }

        return propertyToObject(json);
    }

    /**
     * Returns all T's that are stored against the given issue id.
     *
     * @param issueId the issue id to look up
     * @return a map of Ts mapped by key
     */
    @Nonnull
    public Map<String, Optional<T>> getAllForIssue(long issueId) {
        final List<EntityProperty> jsons = jsonEntityPropertyManager.query()
                .entityName(entityName)
                .entityId(issueId)
                .find();

        final Map<String, Optional<T>> objectByKey = Maps.newHashMapWithExpectedSize(firstNonNull(jsons,
                emptyList()).size());

        for (EntityProperty json : jsons) {
            objectByKey.put(json.getKey(), propertyToObject(json));
        }

        return objectByKey; //jsonEntityPropertyManager.deleteByEntity(entityName, issueId);
    }

    /**
     * Stores a T against the given issue id. It won't do anything if the given value is None
     *
     * @param issueId the issueId to store against
     * @param key     the key for the issue property to store value as
     * @param value   the value to store
     * @throws IllegalArgumentException if value is null or can not be serialised
     * @throws IllegalStateException    if an error is thrown during serialisation or serialisation fails
     */
    public void put(final long issueId, final String key, @Nonnull final Optional<T> value) {
        checkNotNull(value, "value is null. Pass an option.none() instead.");

        final Optional<String> valueAsJson = value.flatMap(input -> {
            try {
                return Optional.of(OBJECT_MAPPER.writeValueAsString(input));
            } catch (IOException e) {
                log.error("Error serialising value to JSON: " + input, e);
                return Optional.empty();
            }
        });

        if (!valueAsJson.isPresent()) {
            log.error("Error serialising value to JSON: " + value.orElse(null));
            throw new IllegalStateException("Value could not be serialised to JSON.");
        }

        //TODO: remove call to deprecated method: https://jira.atlassian.com/browse/HC-10752
        jsonEntityPropertyManager.put(entityName, issueId, key, valueAsJson.get());
    }

    /**
     * Delete a T with the given issue id.
     *
     * @param issueId The issue id to delete from the store
     * @param key     The key, usually source for dev summary
     */
    public void delete(long issueId, String key) {
        jsonEntityPropertyManager.delete(entityName, issueId, key);
    }

    /**
     * Converts a JSON value to a {@code storedClass} using the ObjectMapper.
     *
     * @param entityProperty EntityProperty
     * @return an Optional of T, or None if input is null or an error happens while converting JSON value
     */
    @VisibleForTesting
    @Nonnull
    Optional<T> propertyToObject(final EntityProperty entityProperty) {
        final String jsonObject = entityProperty.getValue();

        final T valueAsObject;

        try {
            valueAsObject = OBJECT_MAPPER.readValue(jsonObject, storedClass);
        } catch (IOException e) {
            log.error("Error deserialising value from JSON: " + jsonObject, e);
            return Optional.empty();
        }

        return Optional.of(valueAsObject);
    }

    @Nonnull
    public List<T> getAll() {
        return findByPredicate(entity -> true);
    }

    public List<T> findByPredicate(Predicate<T> predicate) {
        final List<T> items = new ArrayList<T>();
        jsonEntityPropertyManager.query().entityName(entityName).find(propertyEntity -> {
            Optional<T> entity = propertyToObject(propertyEntity);
            entity.ifPresent(anEntity -> {
                if (predicate.test(anEntity)) {
                    items.add(anEntity);
                }
            });
        });
        return items;
    }

    public void deleteAllForKey(String key) {
        jsonEntityPropertyManager.deleteByEntityNameAndPropertyKey(entityName, key);
    }
}
