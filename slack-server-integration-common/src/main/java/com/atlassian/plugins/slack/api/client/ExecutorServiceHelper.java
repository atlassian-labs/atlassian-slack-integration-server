package com.atlassian.plugins.slack.api.client;

import com.atlassian.util.concurrent.ThreadFactories;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.atlassian.util.concurrent.ThreadFactories.Type.DAEMON;

@Component
public class ExecutorServiceHelper {
    private static final Logger log = LoggerFactory.getLogger(ExecutorServiceHelper.class);
    private static final int DEFAULT_THREAD_POOL_SIZE = 5;
    private static final int DEFAULT_QUEUE_SIZE = 1000;

    private final int threadPoolSize;
    private final int queueSize;

    public ExecutorServiceHelper() {
        threadPoolSize = Integer.getInteger("slack.client.thread.pool.size", DEFAULT_THREAD_POOL_SIZE);
        queueSize = Integer.getInteger("slack.client.queue.size", DEFAULT_QUEUE_SIZE);
    }

    public ExecutorService createBoundedExecutorService() {
        return new ThreadPoolExecutor(threadPoolSize,
                threadPoolSize,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(queueSize),
                ThreadFactories.namedThreadFactory("slack-client", DAEMON),
                (task, executor) -> log.debug("Task '{}' rejected from thread pool executor '{}'.",
                        task.toString(),
                        executor.toString()));
    }
}
