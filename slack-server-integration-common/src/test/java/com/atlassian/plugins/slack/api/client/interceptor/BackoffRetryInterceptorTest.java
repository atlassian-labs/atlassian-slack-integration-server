package com.atlassian.plugins.slack.api.client.interceptor;

import com.github.seratch.jslack.api.methods.MethodsClient;
import com.github.seratch.jslack.api.methods.response.api.ApiTestResponse;
import okhttp3.Interceptor;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@PrepareForTest({BackoffRetryInterceptor.class})
@RunWith(PowerMockRunner.class)
public class BackoffRetryInterceptorTest {
    @Mock
    Interceptor.Chain chain;
    @Mock
    MethodsClient methods;
    @Mock
    ApiTestResponse apiResponse;
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    Request request = new Request.Builder()
            .url("https://example.com")
            .header(RequestIdInterceptor.REQ_ID_HEADER, "xyz")
            .build();
    Response successResponse = respBuilder(200).message("msg").build();
    Response serverErrorResponse = respBuilder(500).message("msg").build();
    Response requestErrorResponse = respBuilder(400).message("msg").build();

    private BackoffRetryInterceptor target;

    @Before
    public void setup() throws InterruptedException {
        PowerMockito.mockStatic(Thread.class);
        PowerMockito.doNothing().when(Thread.class);
        Thread.sleep(anyLong());

        when(chain.request()).thenReturn(request);
    }

    @Test
    public void intercept_shouldProceedWithNormalRequest() throws IOException {
        when(chain.proceed(request)).thenReturn(successResponse);

        target = new BackoffRetryInterceptor();
        Response result = target.intercept(chain);

        assertThat(result, is(successResponse));
        verify(chain).proceed(request);
    }

    @Test
    public void intercept_shouldRetryOnServerError() throws Exception {
        when(chain.proceed(request)).thenReturn(serverErrorResponse, successResponse);

        target = new BackoffRetryInterceptor(2);
        Response result = target.intercept(chain);

        assertThat(result, is(successResponse));
        verify(chain, times(2)).proceed(request);
        verifySleptFor(1000L);
    }

    @Test
    public void intercept_shouldRetryOnRequestGenericException() throws Exception {
        when(chain.proceed(request))
                .thenThrow(new NullPointerException())
                .thenReturn(successResponse);

        target = new BackoffRetryInterceptor(2);
        Response result = target.intercept(chain);

        assertThat(result, is(successResponse));
        verify(chain, times(2)).proceed(request);
        verifySleptFor(1000L);
    }

    @Test
    public void intercept_shouldFailFastOnRequestCanceledException() throws Exception {
        when(chain.proceed(request)).thenThrow(new IOException("Canceled"));
        thrown.expectMessage("Canceled");

        target = new BackoffRetryInterceptor(2);
        target.intercept(chain);
    }

    @Test
    public void intercept_shouldRetryTwoTimesAndReturnError() throws Exception {
        when(chain.proceed(request)).thenReturn(serverErrorResponse, serverErrorResponse, serverErrorResponse);

        target = new BackoffRetryInterceptor(2);
        Response result = target.intercept(chain);

        assertThat(result, is(serverErrorResponse));
        verify(chain, times(3)).proceed(request);
        verifySleptFor(1000L);
        verifySleptFor(3000L);
    }

    @Test
    public void intercept_shouldRetryTenTimesAtDefinedIntervals() throws Exception {
        when(chain.proceed(request)).thenReturn(serverErrorResponse);

        target = new BackoffRetryInterceptor(10);
        Response result = target.intercept(chain);

        assertThat(result, is(serverErrorResponse));
        verify(chain, times(11)).proceed(request);
        verifySleptFor(1000L);
        verifySleptFor(3000L);
        verifySleptFor(5000L);
        verifySleptFor(8000L);
        verifySleptFor(13000L);
        verifySleptFor(30000L, 5);
    }

    @Test
    public void intercept_shouldNotRetryOnNonServerError() throws Exception {
        when(chain.proceed(request)).thenReturn(requestErrorResponse);

        target = new BackoffRetryInterceptor(2);
        Response result = target.intercept(chain);

        assertThat(result, is(requestErrorResponse));
        verify(chain).proceed(request);
    }

    private void verifySleptFor(long ms) throws InterruptedException {
        PowerMockito.verifyStatic(Thread.class);
        Thread.sleep(ms);
    }

    private void verifySleptFor(long ms, int times) throws InterruptedException {
        PowerMockito.verifyStatic(Thread.class, times(times));
        Thread.sleep(ms);
    }

    private Response.Builder respBuilder(int code) {
        return new Response.Builder()
                .protocol(Protocol.HTTP_1_1)
                .request(request)
                .code(code)
                .message("msg");
    }
}
