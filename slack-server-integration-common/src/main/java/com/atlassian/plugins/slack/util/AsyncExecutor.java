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

@Component
public class AsyncExecutor implements InitializingBean, DisposableBean {
    private static final Logger log = LoggerFactory.getLogger(AsyncExecutor.class);

    private final ExecutorServiceHelper executorServiceHelper;
    private final TransactionTemplate transactionTemplate;
    private final ThreadLocalDelegateExecutorFactory threadLocalDelegateExecutorFactory;

    private ExecutorService executorService;

    @Autowired
    public AsyncExecutor(final ExecutorServiceHelper executorServiceHelper,
                         final TransactionTemplate transactionTemplate,
                         final ThreadLocalDelegateExecutorFactory threadLocalDelegateExecutorFactory) {
        this.executorServiceHelper = executorServiceHelper;
        this.transactionTemplate = transactionTemplate;
        this.threadLocalDelegateExecutorFactory = threadLocalDelegateExecutorFactory;
    }

    public void run(final Runnable runnable) {
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
            } catch (Exception e) {
                // nothing
            }
        }
    }
}
