package com.atlassian.jira.plugins.slack.mentions.storage.cache;

import com.atlassian.cache.CacheLoader;
import com.atlassian.cache.CacheManager;
import com.atlassian.jira.plugins.slack.storage.cache.AbstractCacheableEntityManager;
import com.atlassian.jira.plugins.slack.storage.cache.CacheableEntity;
import com.google.common.annotations.VisibleForTesting;
import jakarta.annotation.Nonnull;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public abstract class AbstractIssueMentionCacheManager<K, E extends CacheableEntity<K>> extends AbstractCacheableEntityManager<K, E> {
    @VisibleForTesting
    static final String CACHE_NAME_PREFIX = "issue-mentions-cache";

    AbstractIssueMentionCacheManager(@Nonnull final CacheManager cacheManager,
                                     @Nonnull final CacheLoader<K, Optional<E>> cacheLoader,
                                     @Nonnull final Long ttl,
                                     @Nonnull final TimeUnit unit) {
        super(cacheManager, CACHE_NAME_PREFIX, cacheLoader, ttl, unit);
    }
}
