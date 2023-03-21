package com.atlassian.jira.plugins.slack.service.task.impl;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.concurrent.Callable;

public class CallableToRunnableAdapter implements Runnable {
    private final Callable<?> callable;

    public CallableToRunnableAdapter(@Nonnull Callable<?> callable) {
        this.callable = Objects.requireNonNull(callable);
    }

    @Override
    public void run() {
        try {
            callable.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
