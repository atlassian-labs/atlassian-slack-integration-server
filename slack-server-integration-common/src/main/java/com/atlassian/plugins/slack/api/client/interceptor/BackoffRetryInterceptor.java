package com.atlassian.plugins.slack.api.client.interceptor;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class BackoffRetryInterceptor implements Interceptor {
    private static final int[] RETRY_BACKOFF_DELAYS = new int[]{1, 3, 5, 8, 13, 30};
    private final int retryCount;

    public BackoffRetryInterceptor() {
        this(Integer.getInteger("slack.client.retry.count", 3));
    }

    @VisibleForTesting
    protected BackoffRetryInterceptor(final int retryCount) {
        this.retryCount = retryCount;
    }

    @Override
    public Response intercept(final Chain chain) throws IOException {
        final Request request = chain.request();

        // do not process conversations.info and users.info requests
        final boolean shouldApplyRetries = !request.url().encodedPath().contains(".info");
        if (!shouldApplyRetries) {
            return chain.proceed(request);
        }

        Response response = null;
        for (int tryCount = 0; tryCount <= retryCount; tryCount++) {
            boolean lastAttempt = tryCount >= retryCount;
            try {
                if (response != null && response.body() != null) {
                    try {
                        response.close();
                    } catch (Exception e) {
                        // no-op
                    }
                }

                response = chain.proceed(request);
                if (response.isSuccessful()) {
                    break;
                }

                // do not retry if not a server error
                final int code = response.code();
                if (code < 500 || code >= 600) {
                    break;
                }

                if (!lastAttempt) {
                    log.debug("Will wait and retry on request error:  {} {} -> {}", request.method(), request.url(), code);
                }
            } catch (Exception e) {
                log.debug("Will wait and retry after request error: {}", e.getMessage(), e);

                if ("canceled".equalsIgnoreCase(e.getMessage())) {
                    throw e;
                }

                if (lastAttempt) {
                    throw e;
                }
            }

            if (!lastAttempt) {
                sleepBeforeRetry(tryCount, request);
            }
        }

        return response;
    }

    private void sleepBeforeRetry(final int tryCount, final Request request) {
        final int retryDelay = tryCount >= RETRY_BACKOFF_DELAYS.length
                ? RETRY_BACKOFF_DELAYS[RETRY_BACKOFF_DELAYS.length - 1]
                : RETRY_BACKOFF_DELAYS[tryCount];
        log.info("Failed request to Slack {} {}. Retrying after {}s", request.method(), request.url(), retryDelay);
        sleep(1000L * retryDelay);
    }

    private void sleep(final long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
