package com.atlassian.jira.plugins.slack.service.task.impl;

import com.atlassian.jira.util.thread.JiraThreadLocalUtil;
import lombok.RequiredArgsConstructor;
import org.apache.log4j.Logger;

import java.util.concurrent.Callable;

@RequiredArgsConstructor
public class ThreadLocalAwareTask implements Runnable {
    private static final Logger log4j = Logger.getLogger(ThreadLocalAwareTask.class);

    private final JiraThreadLocalUtil jiraThreadLocalUtil;
    private final Runnable delegateRunnable;

    public ThreadLocalAwareTask(final JiraThreadLocalUtil jiraThreadLocalUtil, final Callable<?> callable) {
        this(jiraThreadLocalUtil, new CallableToRunnableAdapter(callable));
    }

    @Override
    public void run() {
        try {
            jiraThreadLocalUtil.preCall();
            delegateRunnable.run();
        } finally {
            jiraThreadLocalUtil.postCall(log4j);
        }
    }
}
