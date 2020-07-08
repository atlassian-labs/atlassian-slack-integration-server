package com.atlassian.plugins.slack.api;

public class TeamNotConnectedException extends Exception {
    public TeamNotConnectedException(String teamId) {
        super("Team " + teamId + " is not connected");
    }
}
