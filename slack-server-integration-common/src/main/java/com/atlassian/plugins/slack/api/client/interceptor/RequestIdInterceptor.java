package com.atlassian.plugins.slack.api.client.interceptor;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okio.Buffer;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@Slf4j
public class RequestIdInterceptor implements Interceptor {
    public static final String REQ_ID_HEADER = "X-Atlassian-Request-Id";

    @Override
    public Response intercept(final Chain chain) throws IOException {
        final String reqId = UUID.randomUUID().toString();
        final Request request = chain.request().newBuilder()
                .header(REQ_ID_HEADER, reqId)
                .build();

        if (log.isTraceEnabled() && request.body() != null && !request.body().isOneShot()) {
            final Buffer buffer = new Buffer();
            request.body().writeTo(buffer);
            log.trace("Initializing Slack request {} - req_id: {} - body: {}", request.url().encodedPath(), reqId,
                    buffer.readUtf8());
        }

        return chain.proceed(request);
    }
}
