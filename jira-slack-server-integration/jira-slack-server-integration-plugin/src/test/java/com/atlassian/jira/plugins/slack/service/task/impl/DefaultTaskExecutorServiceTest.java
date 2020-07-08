package com.atlassian.jira.plugins.slack.service.task.impl;

import com.atlassian.jira.util.thread.JiraThreadLocalUtil;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.concurrent.Callable;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultTaskExecutorServiceTest {
    @Mock
    private JiraThreadLocalUtil threadLocalUtil;
    @Mock
    private Callable<String> callable;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private DefaultTaskExecutorService target;

    @Test
    public void onSpringContextStarted() {
        target.onSpringContextStarted();
    }

    @Test
    public void onSpringContextStopped() {
        target.onSpringContextStopped();
        //calls twice to make sure it won't break
        target.onSpringContextStopped();

        assertThat(target.executorService.isTerminated(), is(true));
    }

    @Test
    public void submitTask() throws Exception {
        ListenableFuture<String> result = target.submitTask(() -> {
            verify(threadLocalUtil).preCall();
            return "A";
        });

        assertThat(result.get(), is("A"));

        verify(threadLocalUtil).postCall(any());
    }
}
