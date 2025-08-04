package com.atlassian.jira.plugins.slack.mentions.storage.cache;

import com.atlassian.cache.CacheLoader;
import com.atlassian.jira.plugins.slack.storage.cache.CacheableEntity;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import jakarta.annotation.Nonnull;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Abstract CacheLoader that uses Slack API
 */
abstract class SlackApiCacheLoader<K, E extends CacheableEntity<K>, L> implements CacheLoader<K, Optional<E>> {
    private final SlackClientProvider slackClientProvider;

    SlackApiCacheLoader(@Nonnull final SlackClientProvider slackClientProvider) {
        this.slackClientProvider = checkNotNull(slackClientProvider);
    }

    @Nonnull
    @Override
    public Optional<E> load(@Nonnull final K key) {
        try {
            return fetchCacheLoad(key).map(value -> createCacheableEntity(key, value));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected SlackClientProvider getSlackClientProvider() {
        return slackClientProvider;
    }

    /**
     * Create an instance of the entity object to be cached, using the given raw data
     *
     * @param key  the cache key
     * @param load the load entity returned from a call to Slack API that will be saved in the cached entity
     * @return the cacheable entity to be saved in cache and returned by the cache manager
     */
    protected abstract E createCacheableEntity(K key, L load);

    protected abstract Optional<L> fetchCacheLoad(K key) throws Exception;
}
