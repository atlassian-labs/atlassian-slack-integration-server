package com.atlassian.jira.plugins.slack.service.task.impl;

import com.atlassian.jira.util.thread.JiraThreadLocalUtil;
import com.atlassian.plugins.slack.util.AsyncExecutor;
import com.atlassian.plugins.slack.util.AsyncExecutorDelegate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class ThreadLocalAwareAsyncExecutorDelegate implements AsyncExecutorDelegate, InitializingBean {
    private static final Logger log4j = Logger.getLogger(ThreadLocalAwareAsyncExecutorDelegate.class);

    private final JiraThreadLocalUtil jiraThreadLocalUtil;
    private final AsyncExecutor asyncExecutor;

    @Override
    public void run(final Runnable runnable) {
        try {
            jiraThreadLocalUtil.preCall();
            runnable.run();
        } finally {
            jiraThreadLocalUtil.postCall(log4j);
        }
    }

    @Override
    public void afterPropertiesSet() {
        asyncExecutor.setDelegate(this);
        log.debug("ThreadLocalAwareAsyncExecutorDelegate is set");
    }
}
