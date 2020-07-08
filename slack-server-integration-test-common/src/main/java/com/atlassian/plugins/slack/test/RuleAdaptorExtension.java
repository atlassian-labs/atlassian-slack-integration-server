package com.atlassian.plugins.slack.test;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runners.model.TestClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Provide backwards compatibility to some existing JUnit 4 rules from Atlassian we need in WebDriver tests
 */
public class RuleAdaptorExtension implements BeforeEachCallback, AfterEachCallback {
    private static final Logger log = LoggerFactory.getLogger(RuleAdaptorExtension.class);

    private final TestWatcher watcher;
    private final ExternalResource resource;

    public RuleAdaptorExtension(final TestWatcher watcher) {
        this.watcher = watcher;
        this.resource = null;
    }

    public RuleAdaptorExtension(final ExternalResource resource) {
        this.resource = resource;
        this.watcher = null;
    }

    @Override
    public void beforeEach(final ExtensionContext context) {
        invokeOnWatcher("startingQuietly", context, null);
        invokeOnExternalResource("before");
    }

    @Override
    public void afterEach(final ExtensionContext context) {
        if (context.getExecutionException().isPresent()) {
            invokeOnWatcher("failedQuietly", context, context.getExecutionException().get());
        } else {
            invokeOnWatcher("succeededQuietly", context, null);
        }
        invokeOnWatcher("finishedQuietly", context, null);
        invokeOnExternalResource("after");
    }

    private void invokeOnExternalResource(final String methodName) {
        if (resource != null) {
            try {
                ReflectionTestUtils.invokeMethod(resource, methodName);
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    private void invokeOnWatcher(final String methodName,
                                 final ExtensionContext context,
                                 @Nullable final Throwable error) {
        if (watcher != null) {
            try {
                List<Throwable> errors = new ArrayList<>();
                Description description = Description.createTestDescription(
                        context.getRequiredTestClass(),
                        context.getDisplayName(),
                        new TestClass(context.getRequiredTestClass()).getJavaClass().getAnnotations());

                ReflectionTestUtils.invokeMethod(
                        watcher,
                        methodName,
                        error != null ? new Object[]{error, description, errors} : new Object[]{description, errors});

                errors.forEach(e -> log.warn(e.getMessage(), e));
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
        }
    }
}
