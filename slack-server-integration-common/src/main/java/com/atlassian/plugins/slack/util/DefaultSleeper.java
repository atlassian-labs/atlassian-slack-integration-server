package com.atlassian.plugins.slack.util;

public class DefaultSleeper implements Sleeper {
    @Override
    public void sleep(long millis) throws InterruptedException {
        Thread.sleep(millis);
    }
}
