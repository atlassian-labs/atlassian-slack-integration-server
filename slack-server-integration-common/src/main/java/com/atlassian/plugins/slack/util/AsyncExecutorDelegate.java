package com.atlassian.plugins.slack.util;

@FunctionalInterface
public interface AsyncExecutorDelegate {
    void run(Runnable runnable);
}
