package com.atlassian.plugins.slack.util;

import com.github.seratch.jslack.api.methods.SlackApiResponse;
import io.atlassian.fugue.Either;
import okhttp3.Response;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.PropertyNamingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ResponseMapper {
    private static final Logger log = LoggerFactory.getLogger(ResponseMapper.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);

    private ResponseMapper() {
    }

    public static <T extends SlackApiResponse> Either<ErrorResponse, T> toEither(String id, ResponseSupplier<T> supplier) {
        try {
            T resp = supplier.get();
            if (resp.isOk()) {
                log.info("Successful request to Slack: {}", id);
                return Either.right(resp);
            } else {
                final String error = resp.getError();
                // handle gracefully errors that are not really a problem
                if ("already_in_channel".equals(error)) {
                    log.debug("Slack returned an unsuccessful response to {}: {}", id, error);
                } else {
                    final Map<String, String> errorMap = new HashMap<>();
                    errorMap.put("error", error);
                    errorMap.put("needed", resp.getNeeded());
                    errorMap.put("provided", resp.getProvided());
                    errorMap.put("warning", resp.getWarning());
                    log.warn("Slack returned an unsuccessful response to {}: {}", id, errorMap);
                }
                return Either.left(new ErrorResponse(resp));
            }
        } catch (Throwable e) {
            log.warn("Error when performing request to Slack: {}", id, e);
            return Either.left(new ErrorResponse(e));
        }
    }

    public static <T extends Response, R extends SlackApiResponse> Either<ErrorResponse, R> responseToEither(
            String id,
            ResponseSupplier<T> supplier,
            Class<R> clazz) {
        try {
            T resp = supplier.get();
            if (resp.isSuccessful()) {
                log.info("Successful request to Slack: {}", id);
                R value = OBJECT_MAPPER.readValue(Objects.requireNonNull(resp.body()).byteStream(), clazz);
                return Either.right(value);
            } else {
                log.warn("Slack returned an unsuccessful response to {}: {} {}", id, resp.code(), resp.message());
                return Either.left(new ErrorResponse(new Exception("Could not send response to Slack"), resp.code()));
            }
        } catch (Throwable e) {
            log.warn("Error when performing request to Slack: {}", id, e);
            return Either.left(new ErrorResponse(e));
        }
    }

    public static <T extends Response> Either<ErrorResponse, Integer> responseToEither(
            String id,
            ResponseSupplier<T> supplier) {
        try {
            T resp = supplier.get();
            if (resp.isSuccessful()) {
                log.info("Successful request to Slack: {}", id);
                return Either.right(resp.code());
            } else {
                log.warn("Slack returned an unsuccessful response to {}: {} {}", id, resp.code(), resp.message());
                return Either.left(new ErrorResponse(new Exception("Could not send response to Slack"), resp.code()));
            }
        } catch (Throwable e) {
            log.warn("Error when performing request to Slack: {}", id, e);
            return Either.left(new ErrorResponse(e));
        }
    }
}
