package com.atlassian.plugins.slack.api.client.interceptor;

import com.atlassian.plugins.slack.util.DefaultSleeper;
import com.atlassian.plugins.slack.util.Sleeper;
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
    private final Sleeper sleeper;
    private final int retryCount;

    public BackoffRetryInterceptor() {
        this(new DefaultSleeper(), Integer.getInteger("slack.client.retry.count", 3));
    }

    @VisibleForTesting
    protected BackoffRetryInterceptor(Sleeper sleeper, int retryCount) {
        this.sleeper = sleeper;
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
                final int code = response.code();
                if (response.isSuccessful()) {
                    if (tryCount > 0 && log.isDebugEnabled()) {
                        log.debug("Backoff recover req_id={} {} {} -> {} - attempt {}/{}",
                                request.header(RequestIdInterceptor.REQ_ID_HEADER),
                                request.method(),
                                request.url(),
                                code,
                                tryCount + 1,
                                retryCount + 1);
                    }

                    break;
                }

                // do not retry if not a server error
                if (code < 500 || code >= 600) {
                    break;
                }

                if (log.isDebugEnabled()) {
                    log.debug("Backoff retry req_id={} {} {} -> {} - retrying in {}s - attempt {}/{}",
                            request.header(RequestIdInterceptor.REQ_ID_HEADER),
                            request.method(),
                            request.url(),
                            code,
                            retryDelay(tryCount),
                            tryCount + 1,
                            retryCount + 1);
                }
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.debug("Backoff retry error req_id={} {} {} - retrying in {}s - attempt {}/{} - {}",
                            request.header(RequestIdInterceptor.REQ_ID_HEADER),
                            request.method(),
                            request.url(),
                            retryDelay(tryCount),
                            tryCount + 1,
                            retryCount + 1,
                            e.getMessage(),
                            e);
                }

                if ("canceled".equalsIgnoreCase(e.getMessage())) {
                    throw e;
                }

                if (lastAttempt) {
                    throw e;
                }
            }

            if (!lastAttempt) {
                sleep(1000L * retryDelay(tryCount));
            }
        }

        return response;
    }

    private int retryDelay(final int tryCount) {
        return tryCount >= RETRY_BACKOFF_DELAYS.length
                ? RETRY_BACKOFF_DELAYS[RETRY_BACKOFF_DELAYS.length - 1]
                : RETRY_BACKOFF_DELAYS[tryCount];
    }

    private void sleep(final long ms) {
        try {
            sleeper.sleep(ms);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
