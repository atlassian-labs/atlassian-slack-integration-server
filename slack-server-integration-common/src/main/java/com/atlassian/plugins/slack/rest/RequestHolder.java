package com.atlassian.plugins.slack.rest;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class RequestHolder {
    private final Cache<String, CachingServletRequestWrapper> store;

    public RequestHolder() {
        store = CacheBuilder.newBuilder()
                .expireAfterWrite(1L, TimeUnit.MINUTES)
                .maximumSize(1000L)
                .build();
    }

    public void put(final String signature, final CachingServletRequestWrapper request) {
        store.put(signature, request);
    }

    public Optional<CachingServletRequestWrapper> getAndRemove(final String signature) {
        Optional<CachingServletRequestWrapper> value = Optional.ofNullable(store.getIfPresent(signature));
        value.ifPresent(v -> store.invalidate(signature));
        return value;
    }
}
