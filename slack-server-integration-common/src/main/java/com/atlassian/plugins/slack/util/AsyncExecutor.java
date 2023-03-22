package com.atlassian.plugins.slack.util;

import com.atlassian.plugins.slack.api.client.ExecutorServiceHelper;
import com.atlassian.sal.api.executor.ThreadLocalDelegateExecutorFactory;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class AsyncExecutor implements InitializingBean, DisposableBean {
    private final ExecutorServiceHelper executorServiceHelper;
    private final TransactionTemplate transactionTemplate;
    private final ThreadLocalDelegateExecutorFactory threadLocalDelegateExecutorFactory;

    @Setter @Getter
    private AsyncExecutorDelegate delegate;
    private ExecutorService executorService;

    public void run(final Runnable runnable) {
        executorService.submit(() -> {
            try {
                transactionTemplate.execute(() -> {
                    if (delegate != null) {
                        delegate.run(runnable);
                    } else {
                        runnable.run();
                    }

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
