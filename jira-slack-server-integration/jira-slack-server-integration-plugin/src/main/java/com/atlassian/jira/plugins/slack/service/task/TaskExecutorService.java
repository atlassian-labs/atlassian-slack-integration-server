package com.atlassian.jira.plugins.slack.service.task;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Callable;

/**
 * Responsible for asynchronously performing 'slack related' tasks on behalf of the host application -- mostly or
 * entirely the processing of Jira events into Slack notifications.
 * <p>
 * Responsible for: - recording metrics - throttling - cleanly shutting down when the plugin is disabled
 */
public interface TaskExecutorService {

    /**
     * Submits a task to the thread pool
     *
     * @param job the job
     * @param <A> the type of returned value
     * @return a future
     */
    <A> ListenableFuture<A> submitTask(Callable<A> job);
}
