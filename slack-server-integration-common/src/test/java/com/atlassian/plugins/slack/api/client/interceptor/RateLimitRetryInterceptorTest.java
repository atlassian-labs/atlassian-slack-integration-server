package com.atlassian.plugins.slack.api.client.interceptor;

import com.github.seratch.jslack.api.methods.MethodsClient;
import com.github.seratch.jslack.api.methods.response.api.ApiTestResponse;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RateLimitRetryInterceptorTest {
    @Mock
    Interceptor.Chain chain;
    @Mock
    MethodsClient methods;
    @Mock
    ApiTestResponse apiResponse;
    @Mock
    BackoffRetryInterceptor.Sleeper mockSleeper;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    Request postRequest = new Request.Builder()
            .post(RequestBody.create(MediaType.get("text/plain"), "test"))
            .url("https://example.com").build();
    Request infoRequest = new Request.Builder().url("https://example.com/conversations.info").build();
    Response successResponse = respBuilder(200).message("msg").build();
    Response rateLimitWithoutHeaderResponse = respBuilder(429).build();
    Response rateLimitResponse1 = respBuilder(429).header("Retry-After", "10").build();
    Response rateLimitResponse2 = respBuilder(429).header("Retry-After", "5").build();

    RateLimitRetryInterceptor target;

    @Before
    public void setup() throws InterruptedException {
        target = new RateLimitRetryInterceptor(3, mockSleeper);
    }

    @Test
    public void intercept_shouldProceedWithNormalGetRequest() throws IOException {
        when(chain.request()).thenReturn(infoRequest);
        when(chain.proceed(infoRequest)).thenReturn(successResponse);

        Response result = target.intercept(chain);

        assertThat(result, is(successResponse));
        verify(chain).proceed(infoRequest);
    }

    @Test
    public void intercept_shouldProceedWithNormalPostRequest() throws IOException {
        when(chain.request()).thenReturn(postRequest);
        when(chain.proceed(postRequest)).thenReturn(successResponse);

        Response result = target.intercept(chain);

        assertThat(result, is(successResponse));
        verify(chain).proceed(postRequest);
    }

    @Test
    public void intercept_failsFastOnRateLimitInGetInfoRequest() throws IOException {
        when(chain.request()).thenReturn(infoRequest);
        when(chain.proceed(infoRequest)).thenReturn(rateLimitResponse1);

        Response result = target.intercept(chain);

        assertThat(result, is(rateLimitResponse1));
        verify(chain).proceed(infoRequest);
    }

    @Test
    public void intercept_failsFastOnRateLimitWithoutRetryAfter() throws IOException {
        when(chain.request()).thenReturn(postRequest);
        when(chain.proceed(postRequest)).thenReturn(rateLimitWithoutHeaderResponse);

        Response result = target.intercept(chain);

        assertThat(result, is(rateLimitWithoutHeaderResponse));
        verify(chain).proceed(postRequest);
    }

    @Test
    public void intercept_failsFastOnRateLimitWhenSpeeInterrupted() throws IOException, InterruptedException {
        doThrow(new InterruptedException()).when(mockSleeper).sleep(anyLong());

        when(chain.request()).thenReturn(postRequest);
        when(chain.proceed(postRequest)).thenReturn(rateLimitResponse1);

        Response result = target.intercept(chain);

        assertThat(result, is(rateLimitResponse1));
        verify(chain).proceed(postRequest);
        verify(mockSleeper).sleep(10_000L);
    }

    @Test
    public void intercept_shouldWaitForPostRequestAccordingToRetryAfter() throws Exception {
        when(chain.request()).thenReturn(postRequest);
        when(chain.proceed(postRequest)).thenReturn(rateLimitResponse1, rateLimitResponse2, successResponse);

        Response result = target.intercept(chain);

        assertThat(result, is(successResponse));
        verify(chain, times(3)).proceed(postRequest);
        verifySleptFor(10_000L);
        verifySleptFor(5_000L);
    }

    @Test
    public void intercept_shouldFailAfterMaxAttempts() throws Exception {
        when(chain.request()).thenReturn(postRequest);
        when(chain.proceed(postRequest)).thenReturn(rateLimitResponse1, rateLimitResponse2, rateLimitResponse1);

        Response result = target.intercept(chain);

        assertThat(result, is(rateLimitResponse1));
        verify(chain, times(3)).proceed(postRequest);
        verifySleptFor(10_000L);
        verifySleptFor(5_000L);
    }

    private void verifySleptFor(long ms) throws InterruptedException {
        verify(mockSleeper).sleep(ms);
    }

    private Response.Builder respBuilder(int code) {
        return new Response.Builder()
                .protocol(Protocol.HTTP_1_1)
                .request(infoRequest)
                .code(code)
                .message("msg");
    }
}
