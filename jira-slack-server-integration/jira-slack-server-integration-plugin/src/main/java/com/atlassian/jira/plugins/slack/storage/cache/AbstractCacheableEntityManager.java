package com.atlassian.jira.plugins.slack.storage.cache;

import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheLoader;
import com.atlassian.cache.CacheManager;
import com.atlassian.cache.CacheSettingsBuilder;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;

public abstract class AbstractCacheableEntityManager<K, E extends CacheableEntity<K>> implements CacheableEntityManager<K, E> {
    private static final Logger log = LoggerFactory.getLogger(AbstractCacheableEntityManager.class);

    private final Cache<K, Optional<E>> cache;

    public AbstractCacheableEntityManager(@Nonnull final CacheManager cacheManager,
                                          @Nonnull final String cacheNamePrefix,
                                          @Nonnull final CacheLoader<K, Optional<E>> cacheLoader,
                                          @Nonnull final Long ttl,
                                          @Nonnull final TimeUnit unit) {
        checkArgument(!isNullOrEmpty(cacheNamePrefix));
        this.cache = cacheManager.getCache(cacheNamePrefix + "." + getEntityType().getName(),
                checkNotNull(cacheLoader),
                new CacheSettingsBuilder()
                        .expireAfterWrite(ttl, unit)
                        .build());
    }

    @Nonnull
    @Override
    public Iterable<K> getKeys() {
        final Collection<K> keys = cache.getKeys();
        log.debug("Getting all entity keys stored in cache: ", keys);
        return ImmutableList.copyOf(keys);
    }

    @Nonnull
    @Override
    public Iterable<E> getAll() {
        return cache.getKeys().stream()
                .map(this::get)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    @Nonnull
    @Override
    public Optional<E> get(@Nonnull final K key) {
        return cache.get(key);
    }

    @Override
    public void put(@Nonnull final K key, @Nonnull final Optional<E> entity) {
        checkNotNull(entity, "entity can not be null. Use Option.none() instead.");
        cache.put(key, entity);
    }

    @Override
    public void delete(@Nonnull K key) {
        cache.remove(key);
    }

    @Override
    public void deleteAll() {
        cache.removeAll();
    }

    protected abstract Class<E> getEntityType();
}
