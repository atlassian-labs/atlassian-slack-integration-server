package com.atlassian.jira.plugins.slack.service.task.impl;

import com.atlassian.jira.plugins.slack.service.task.TaskExecutorService;
import com.atlassian.jira.util.thread.JiraThreadLocalUtil;
import com.atlassian.sal.api.net.ResponseException;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.atlassian.util.concurrent.ThreadFactories.Type.DAEMON;
import static com.atlassian.util.concurrent.ThreadFactories.namedThreadFactory;

@Service
public class DefaultTaskExecutorService implements TaskExecutorService {
    private static final Logger log = LoggerFactory.getLogger(DefaultTaskExecutorService.class);

    private static final int MAX_NUMBER_OF_THREADS = 5;
    private static final int WAIT_BEFORE_RESUBMIT_IN_MILLIS = 3000;
    private static final int QUEUE_CAPACITY = 1000;

    private static final org.apache.log4j.Logger log4j = org.apache.log4j.Logger.getLogger(DefaultTaskExecutorService.class);

    private final JiraThreadLocalUtil threadLocalUtil;

    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    final ListeningExecutorService executorService =
            MoreExecutors.listeningDecorator(new ThreadPoolExecutor(MAX_NUMBER_OF_THREADS,
                    MAX_NUMBER_OF_THREADS,
                    0L,
                    TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(QUEUE_CAPACITY),
                    namedThreadFactory("jira-slack-plugin", DAEMON),
                    // RejectedExecutionHandler implementation that suppresses the exception that
                    // would be thrown in case of a task being rejected and logs a DEBUG message instead.
                    (task, executor) -> {
                        log.debug("Task '{}' rejected from thread pool executor '{}'. Re-submitting now...",
                                task.toString(),
                                executor.toString());

                        try {
                            Thread.sleep(WAIT_BEFORE_RESUBMIT_IN_MILLIS);
                        } catch (InterruptedException e) {
                            log.warn("Thread sleep interrupted.", e);
                            Thread.currentThread().interrupt();
                        }

                        // Re-try if the executor isn't shut down
                        if (!executor.isShutdown()) {
                            executor.execute(task);
                        }
                    })
            );

    @Autowired
    public DefaultTaskExecutorService(final JiraThreadLocalUtil threadLocalUtil) {
        this.threadLocalUtil = threadLocalUtil;
    }

    @PostConstruct
    public void onSpringContextStarted() {
    }

    @PreDestroy
    public void onSpringContextStopped() {
        if (shutdown.compareAndSet(false, true)) {
            shutdown();
        }
    }

    @Override
    public <A> ListenableFuture<A> submitTask(final Callable<A> job) {
        if (shutdown.get()) {
            log.info("TaskExecutorService has been shut down. Submitted task will not be executed: {}", job);
            return Futures.immediateFailedFuture(new IllegalStateException("TaskExecutorService has been shut down."));
        }

        return executorService.submit(() -> {
            threadLocalUtil.preCall();
            try {
                return job.call();
            } catch (ResponseException e) {
                throw e;
            } catch (Throwable e) {
                // catch Throwable because some Errors are useful to log here -- e.g. NoSuchMethodError
                // other exceptions are more likely to be unexpected and worth logging in detail
                log.error("Error running task.", e);
                throw e;
            } finally {
                threadLocalUtil.postCall(log4j);
            }
        });
    }

    private void shutdown() {
        // shutdown thread pool
        do {
            executorService.shutdown(); // Disable new tasks from being submitted

            try {
                // Wait a while for existing tasks to terminate
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow(); // Cancel currently executing tasks
                    // Wait a while for tasks to respond to being cancelled
                    if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                        throw new RuntimeException("Slack plugin executor did not terminate");
                    }
                }
            } catch (InterruptedException ie) {
                // (Re-)Cancel if current thread also interrupted
                executorService.shutdownNow();
                // Preserve interrupt status
                Thread.currentThread().interrupt();
            }
        }
        while (!executorService.isTerminated());
    }
}
