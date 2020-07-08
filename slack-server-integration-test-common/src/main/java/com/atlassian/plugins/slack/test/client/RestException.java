package com.atlassian.plugins.slack.test.client;

import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

public class RestException extends RuntimeException {
    private final Response response;
    private String body;

    public RestException(Response response) {
        try {
            body = response.body() != null ? response.body().string() : "";
        } catch (IOException e) {
            body = "<error>";
        } finally {
            if (response.body() != null) {
                try {
                    response.body().close();
                } catch (Exception e) {
                    // no-op
                }
            }
        }
        this.response = response;
    }

    public Response getResponse() {
        return response;
    }

    public int code() {
        return response.code();
    }

    public String body() {
        return body;
    }

    @Override
    public String toString() {
        return "Request error " + code() + ": " + StringUtils.defaultIfBlank(body(), "<empty>");
    }
}
