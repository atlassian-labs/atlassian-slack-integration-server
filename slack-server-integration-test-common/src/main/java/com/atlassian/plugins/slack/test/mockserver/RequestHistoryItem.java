package com.atlassian.plugins.slack.test.mockserver;

import com.github.seratch.jslack.api.methods.SlackApiRequest;
import okhttp3.mockwebserver.RecordedRequest;

import javax.annotation.Nullable;

import static org.apache.commons.lang3.StringUtils.substring;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;

public class RequestHistoryItem {
    private final RecordedRequest request;
    private final String body;
    private SlackApiRequest parsedEntity;

    public RequestHistoryItem(final RecordedRequest request, final String body) {
        this.request = request;
        this.body = body;
    }

    public void setParsedEntity(final SlackApiRequest parsedEntity) {
        this.parsedEntity = parsedEntity;
    }

    @Nullable
    public SlackApiRequest parsedEntity() {
        return parsedEntity;
    }

    public RecordedRequest request() {
        return request;
    }

    public String body() {
        return body;
    }

    public String contentType() {
        return request.getHeader("content-type");
    }

    public String apiMethod() {
        return substring(request.getPath(), 1);
    }

    public String bearerToken() {
        return substringBefore(authHeader(), "#");
    }

    public String tag() {
        return substringAfter(authHeader(), "#");
    }

    private String authHeader() {
        return substring(request.getHeader("Authorization"), 7);
    }

    @Override
    public String toString() {
        return "RequestHistoryItem{" +
                "request=" + request +
                ", body='" + body + '\'' +
                ", tag='" + tag() + '\'' +
                ", parsedEntity=" + parsedEntity +
                '}';
    }
}
