package com.atlassian.plugins.slack.api;

public class UserNotLinkedException extends Exception {
    public UserNotLinkedException(String userId) {
        super("User " + userId + " has not linked a Slack account");
    }
}
