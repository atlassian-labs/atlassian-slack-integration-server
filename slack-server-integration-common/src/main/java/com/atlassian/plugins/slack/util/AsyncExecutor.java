package com.atlassian.plugins.slack.util;

import com.atlassian.plugins.slack.api.client.ExecutorServiceHelper;
import com.atlassian.sal.api.executor.ThreadLocalDelegateExecutorFactory;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class AsyncExecutor implements InitializingBean, DisposableBean {
    private static final Logger log = LoggerFactory.getLogger(AsyncExecutor.class);

    private final ExecutorServiceHelper executorServiceHelper;
    private final TransactionTemplate transactionTemplate;
    private final ThreadLocalDelegateExecutorFactory threadLocalDelegateExecutorFactory;

    private ExecutorService executorService;
    private ScheduledExecutorService delayingExecutor;

    @Autowired
    public AsyncExecutor(final ExecutorServiceHelper executorServiceHelper,
                         final TransactionTemplate transactionTemplate,
                         final ThreadLocalDelegateExecutorFactory threadLocalDelegateExecutorFactory) {
        this.executorServiceHelper = executorServiceHelper;
        this.transactionTemplate = transactionTemplate;
        this.threadLocalDelegateExecutorFactory = threadLocalDelegateExecutorFactory;
    }

    public void run(final Runnable runnable) {
        log.debug("Submitting Slack job: {}", runnable);

        executorService.submit(() -> {
            try {
                transactionTemplate.execute(() -> {
                    runnable.run();
                    return null;
                });
            } catch (Throwable e) {
                log.error("Failed to perform async task: " + e.getMessage(), e);
            }
        });
    }

    public void runDelayed(final Runnable runnable, final long delay, final TimeUnit timeUnit) {
        log.debug("Scheduling Slack job with delay {}ms: {}", delay, runnable);

        getDelayingExecutor().schedule(() -> this.run(runnable), delay, timeUnit);
    }

    // create a delaying executor lazily to save some resources for Confluence and Bitbucket plugins that don't use it
    private ScheduledExecutorService getDelayingExecutor() {
        if (delayingExecutor == null) {
            delayingExecutor = Executors.newSingleThreadScheduledExecutor();
        }

        return delayingExecutor;
    }

    @Override
    public void afterPropertiesSet() {
        executorService = threadLocalDelegateExecutorFactory
                .createExecutorService(executorServiceHelper.createBoundedExecutorService());
    }

    @Override
    public void destroy() {
        if (executorService != null) {
            try {
                executorService.shutdown();
                if (delayingExecutor != null) {
                    delayingExecutor.shutdown();
                }
            } catch (Exception e) {
                // nothing
            }
        }
    }
}
