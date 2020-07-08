package com.atlassian.plugins.slack.api.client.cache;

import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheException;
import com.atlassian.cache.CacheManager;
import com.atlassian.cache.CacheSettingsBuilder;
import com.atlassian.plugins.slack.util.ResponseSupplier;
import com.github.seratch.jslack.api.methods.SlackApiResponse;
import com.github.seratch.jslack.common.json.GsonFactory;
import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

@Component
@Slf4j
public class SlackResponseCache {
    private final Cache<String, String> cache;
    private final Gson gson = GsonFactory.createSnakeCase();

    @Autowired
    public SlackResponseCache(final CacheManager cacheManager) {
        final CacheSettingsBuilder cacheSettings = new CacheSettingsBuilder()
                .remote()
                .flushable()
                .replicateAsynchronously()
                .replicateViaCopy()
                .expireAfterWrite(Integer.getInteger("slack.client.cache.expire.seconds", 15 * 60), TimeUnit.SECONDS);
        if (Boolean.getBoolean("atlassian.dev.mode")) {
            cacheSettings.statisticsEnabled();
        }
        cache = cacheManager.getCache(SlackResponseCache.class.getName() + ".response.cache", null, cacheSettings.build());
    }

    public <T extends SlackApiResponse> T getAndCacheIfSuccessful(final String token,
                                                                  final String method,
                                                                  final String cacheKey,
                                                                  final ResponseSupplier<T> loader,
                                                                  final Class<T> clazz) throws Exception {
        final String key = encode(token) + "|" + method + "|" + cacheKey;
        final String serializedResponse;
        try {
            serializedResponse = cache.get(key, () -> {
                final SlackApiResponse response;
                try {
                    response = loader.get();
                } catch (Exception e) {
                    throw new WrappedException(e);
                }

                // wraps error response in Exception to be returned below
                if (!response.isOk()) {
                    throw new ApiNotOkException(response);
                }

                return gson.toJson(response);
            });
        } catch (CacheException e) {
            // unwrap exception
            Throwable cause = defaultIfNull(e.getCause(), e);

            // return original response if API error
            if (cause instanceof ApiNotOkException) {
                //noinspection unchecked
                return (T) ((ApiNotOkException) cause).getErrorResponse();
            }

            // throw original if wrapped exception
            if (cause instanceof WrappedException) {
                //noinspection
                throw ((WrappedException) cause).getOriginal();
            }

            // for other errors, just bubble them
            log.warn(e.getMessage(), e);
            throw e;
        }

        try {
            return gson.fromJson(serializedResponse, clazz);
        } catch (JsonParseException e) {
            // clean cache for key to make we are not storing something that cannot be de-serialized
            cache.remove(key);

            // call supplier if there is a problem when de-serializing a cached value
            log.warn(e.getMessage(), e);
            return loader.get();
        }
    }

    private String encode(final String token) {
        return Hashing.sha256().hashString(token, StandardCharsets.UTF_8).toString();
    }

    @EqualsAndHashCode(callSuper = true)
    @Value
    public static class ApiNotOkException extends RuntimeException {
        private SlackApiResponse errorResponse;
    }

    @EqualsAndHashCode(callSuper = true)
    @Value
    private class WrappedException extends RuntimeException {
        private Exception original;
    }
}
