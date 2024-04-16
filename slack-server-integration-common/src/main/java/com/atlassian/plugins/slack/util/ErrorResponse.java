package com.atlassian.plugins.slack.util;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.seratch.jslack.api.methods.SlackApiException;
import com.github.seratch.jslack.api.methods.SlackApiResponse;
import com.google.common.base.Throwables;
import io.atlassian.fugue.Either;
import io.atlassian.fugue.Eithers;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = ANY)
public class ErrorResponse {
    private final static int BAD_GATEWAY = 502;
    private final static int SERVICE_UNAVAILABLE = 503;
    private final static int INTERNAL_SERVER_ERROR = 500;

    @JsonIgnore
    private final Throwable exception;
    @JsonIgnore
    private final SlackApiResponse apiResponse;
    private final int statusCode;
    private final String message;

    public ErrorResponse(Throwable throwable, int statusCode) {
        this(throwable, null, statusCode, throwable.getMessage());
    }

    public ErrorResponse(Throwable throwable) {
        this(throwable, null, extractStatusCodeFromThrowable(throwable), throwable.getMessage());
    }

    public ErrorResponse(SlackApiResponse response) {
        this(new Exception("Request to Slack failed: " + response.getError()), response, BAD_GATEWAY, response.getError());
    }

    public ErrorResponse(final Throwable exception, final SlackApiResponse apiResponse, final int statusCode,
                         final String message) {
        this.exception = exception;
        this.apiResponse = apiResponse;
        this.statusCode = statusCode;
        this.message = message;
    }

    private static int extractStatusCodeFromThrowable(Throwable throwable) {
        if (throwable != null) {
            final Throwable rootCause = Throwables.getRootCause(throwable);
            if (rootCause instanceof SocketTimeoutException || rootCause instanceof ConnectException) {
                return SERVICE_UNAVAILABLE;
            } else if (throwable instanceof SlackApiException) {
                return ((SlackApiException) throwable).getResponse().code();
            }
        }
        return INTERNAL_SERVER_ERROR;
    }

    public Throwable getException() {
        return exception;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getMessage() {
        return message;
    }

    public <T extends SlackApiResponse> Either<Throwable, T> getApiResponse(Class<T> clazz) {
        return Eithers.cond(
                apiResponse != null && clazz.isAssignableFrom(apiResponse.getClass()),
                exception,
                clazz.cast(apiResponse));
    }

    @Override
    public String toString() {
        return "ErrorResponse{" +
                "message=" + message +
                ", statusCode=" + statusCode +
                '}';
    }
}
