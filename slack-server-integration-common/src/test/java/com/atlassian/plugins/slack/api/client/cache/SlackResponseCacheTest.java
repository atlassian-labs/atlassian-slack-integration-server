package com.atlassian.plugins.slack.api.client.cache;

import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheException;
import com.atlassian.cache.CacheLoader;
import com.atlassian.cache.CacheManager;
import com.atlassian.cache.Supplier;
import com.atlassian.plugins.slack.util.ResponseSupplier;
import com.github.seratch.jslack.api.methods.SlackApiException;
import com.github.seratch.jslack.api.methods.response.api.ApiTestResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SlackResponseCacheTest {
    private static final String TOKEN = "tkn";
    private static final String METHOD = "m";
    private static final String CHANNEL = "C";
    private static final String TOKEN_SHA_256 = "d96f62ea0f2f543aa7822a58114f75dbcc05bdf970fb15eb55eea836a1439e43";
    private static final String KEY = TOKEN_SHA_256 + "|" + METHOD + "|" + CHANNEL;
    private static final String SERIALIZED_RESPONSE = "{\"ok\":true}";

    @Mock
    CacheManager cacheManager;
    @Mock
    Cache<String, String> cache;
    @Mock
    ResponseSupplier<ApiTestResponse> supplier;
    @Mock
    SlackApiException slackApiException;

    @Captor
    ArgumentCaptor<Supplier<String>> cacheSupplier;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    ApiTestResponse response = new ApiTestResponse();
    private SlackResponseCache target;

    @Before
    public void setup() {
        when(cacheManager.getCache(anyString(), (CacheLoader<String, String>) any(), any())).thenReturn(cache);
        target = new SlackResponseCache(cacheManager);
    }

    @Test
    public void getAndCacheIfSuccessful_shouldCallSupplierIfCacheIsEmpty() throws Exception {
        mockCacheGet();
        when(supplier.get()).thenReturn(response);
        response.setOk(true);

        final ApiTestResponse result = target.getAndCacheIfSuccessful(TOKEN, METHOD, CHANNEL, supplier, ApiTestResponse.class);

        assertThat(result, is(response));
        verify(supplier).get();
    }

    @Test
    public void getAndCacheIfSuccessful_supplierMustReturnSerializedValue() throws Exception {
        mockCacheGet();
        when(supplier.get()).thenReturn(response);
        response.setOk(true);

        target.getAndCacheIfSuccessful(TOKEN, METHOD, CHANNEL, supplier, ApiTestResponse.class);
        final String result = cacheSupplier.getValue().get();

        assertThat(result, is(SERIALIZED_RESPONSE));
    }

    @Test
    public void getAndCacheIfSuccessful_shouldWrapApiErrorInSupplier() throws Exception {
        when(cache.get(eq(KEY), cacheSupplier.capture())).thenReturn(SERIALIZED_RESPONSE);
        when(supplier.get()).thenReturn(response);
        response.setOk(false);

        target.getAndCacheIfSuccessful(TOKEN, METHOD, CHANNEL, supplier, ApiTestResponse.class);

        thrown.expect(hasProperty("errorResponse", is(response)));
        cacheSupplier.getValue().get();
    }

    @Test
    public void getAndCacheIfSuccessful_shouldUnwrapApiErrorFromSupplier() throws Exception {
        when(cache.get(eq(KEY), any())).thenThrow(new CacheException(new SlackResponseCache.ApiNotOkException(response)));

        final ApiTestResponse result = target.getAndCacheIfSuccessful(TOKEN, METHOD, CHANNEL, supplier, ApiTestResponse.class);
        assertThat(result, is(response));
    }

    @Test
    public void getAndCacheIfSuccessful_shouldNotCallSupplierIfCacheHasValue() throws Exception {
        when(cache.get(eq(KEY), any())).thenReturn(SERIALIZED_RESPONSE);
        response.setOk(true);

        final ApiTestResponse result = target.getAndCacheIfSuccessful(TOKEN, METHOD, CHANNEL, supplier, ApiTestResponse.class);

        assertThat(result, is(response));
        verify(supplier, never()).get();
    }

    @Test
    public void getAndCacheIfSuccessful_shouldThrowErrorFromSupplier() throws Exception {
        mockCacheGet();
        when(supplier.get()).thenThrow(new RuntimeException("boo"));

        thrown.expectMessage("boo");
        target.getAndCacheIfSuccessful(TOKEN, METHOD, CHANNEL, supplier, ApiTestResponse.class);
    }

    @Test
    public void getAndCacheIfSuccessful_shouldCallSupplierOnDeserializationError() throws Exception {
        when(cache.get(eq(KEY), any())).thenReturn("{");
        when(supplier.get()).thenReturn(response);

        final ApiTestResponse result = target.getAndCacheIfSuccessful(TOKEN, METHOD, CHANNEL, supplier, ApiTestResponse.class);

        assertThat(result, is(response));
        verify(cache).remove(KEY);
    }

    private void mockCacheGet() {
        when(cache.get(eq(KEY), cacheSupplier.capture())).thenAnswer(a -> {
            try {
                return ((Supplier<?>) a.getArgument(1)).get();
            } catch (Exception e) {
                throw new CacheException(e);
            }
        });
    }
}
