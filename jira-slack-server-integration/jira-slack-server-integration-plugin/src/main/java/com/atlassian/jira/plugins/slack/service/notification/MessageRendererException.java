package com.atlassian.jira.plugins.slack.service.notification;

public class MessageRendererException extends RuntimeException {
    public MessageRendererException(String message, Throwable cause) {
        super(message, cause);
    }

    public MessageRendererException(String message) {
        super(message);
    }

    public MessageRendererException(Throwable cause) {
        super(cause);
    }
}
