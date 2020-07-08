package com.atlassian.plugins.slack.util;

public interface ResponseSupplier<T> {
    T get() throws Exception;
}
