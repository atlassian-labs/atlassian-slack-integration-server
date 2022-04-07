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
public class RateLimitRetryInterceptor implements Interceptor {
    private final int retryCount;

    public RateLimitRetryInterceptor() {
        this(Integer.getInteger("slack.client.rate.limit.retry.count", 3));
    }

    @VisibleForTesting
    protected RateLimitRetryInterceptor(final int retryCount) {
        this.retryCount = retryCount;
    }

    @Override
    public Response intercept(final Chain chain) throws IOException {
        final Request request = chain.request();

        // skip if not posting something
        final boolean shouldPostponeRequest = !request.url().encodedPath().contains(".info");
        if (!shouldPostponeRequest) {
            return chain.proceed(request);
        }

        Response response = null;
        for (int tryCount = 0; tryCount < retryCount; tryCount++) {
            if (response != null && response.body() != null) {
                try {
                    response.close();
                } catch (Exception e) {
                    // no-op
                }
            }

            response = chain.proceed(request);
            final int code = response.code();
            if (response.isSuccessful() && log.isDebugEnabled()) {
                if (tryCount > 0) {
                    log.debug("Rate-limit recover req_id={} {} {} -> {} - attempt {}/{}",
                            request.header(RequestIdInterceptor.REQ_ID_HEADER),
                            request.method(),
                            request.url(),
                            code,
                            tryCount + 1,
                            retryCount + 1);
                }

                break;
            }

            final String retryAfterStr = response.header("Retry-After");

            if (code == 429 && log.isDebugEnabled()) {
                log.debug("Rate-limited request req_id={} {} {} - {} - attempt {}/{}",
                        request.header(RequestIdInterceptor.REQ_ID_HEADER),
                        request.method(),
                        request.url(),
                        retryAfterStr == null ? "not retrying" : "retrying in " + retryAfterStr + "s",
                        tryCount + 1,
                        retryCount + 1);
            }

            // check if rate limit was reached
            final boolean isRateLimitActive = code == 429 && retryAfterStr != null;
            if (!isRateLimitActive) {
                break;
            }

            // wait according to retry-after
            final boolean lastAttempt = tryCount == retryCount - 1;
            if (!lastAttempt) {
                if (!waitRetryAfterTime(retryAfterStr)) {
                    // fail fast if an interruption occurred or another error
                    break;
                }
            }
        }

        return response;
    }

    private boolean waitRetryAfterTime(final String secondsStr) {
        try {
            Thread.sleep(Long.parseLong(secondsStr) * 1000);
            return true;
        } catch (Exception e) {
            log.warn("Error awaiting for Retry-After: {}", e.getMessage(), e);
            return false;
        }
    }

}
