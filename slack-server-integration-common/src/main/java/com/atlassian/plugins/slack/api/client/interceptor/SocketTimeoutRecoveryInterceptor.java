package com.atlassian.plugins.slack.api.client.interceptor;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.net.SocketTimeoutException;

@Slf4j
public class SocketTimeoutRecoveryInterceptor implements Interceptor {
    private final OkHttpClient client;

    public SocketTimeoutRecoveryInterceptor(final OkHttpClient client) {
        this.client = client;
    }

    @Override
    public Response intercept(final Chain chain) throws IOException {
        final Request request = chain.request();
        try {
            return chain.proceed(request);
        } catch (SocketTimeoutException e) {
            log.info("Recovering from SocketTimeoutException. req_id={}",
                    request.header(RequestIdInterceptor.REQ_ID_HEADER));
            this.client.dispatcher().cancelAll();
            this.client.connectionPool().evictAll();

            throw e;
        }
    }
}
