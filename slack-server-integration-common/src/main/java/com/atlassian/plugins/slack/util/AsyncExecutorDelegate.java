package com.atlassian.plugins.slack.util;

public interface AsyncExecutorDelegate {
    void run(Runnable runnable);
}
