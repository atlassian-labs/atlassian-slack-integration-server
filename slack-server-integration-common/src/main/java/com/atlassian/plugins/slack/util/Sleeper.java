package com.atlassian.plugins.slack.util;

public interface Sleeper {
    void sleep(long millis) throws InterruptedException;
}
