package com.atlassian.plugins.slack.util;

import com.atlassian.plugins.slack.api.client.ExecutorServiceHelper;
import com.atlassian.sal.api.executor.ThreadLocalDelegateExecutorFactory;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.concurrent.ExecutorService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AsyncExecutorTest {
    @Mock
    private ExecutorServiceHelper executorServiceHelper;
    @Mock
    private TransactionTemplate transactionTemplate;
    @Mock
    private ThreadLocalDelegateExecutorFactory threadLocalDelegateExecutorFactory;

    @Mock
    private ExecutorService executorService;
    @Mock
    private Runnable runnable;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private AsyncExecutor target;

    @Test
    public void afterPropertiesSet_shouldCallExpectedMethods() {
        when(executorServiceHelper.createBoundedExecutorService()).thenReturn(executorService);

        target.afterPropertiesSet();

        verify(threadLocalDelegateExecutorFactory).createExecutorService(executorService);
    }

    @Test
    public void destroy_shouldCallExpectedMethodsAndShutDown() {
        when(executorServiceHelper.createBoundedExecutorService()).thenReturn(executorService);
        when(threadLocalDelegateExecutorFactory.createExecutorService(executorService)).thenReturn(executorService);

        target.afterPropertiesSet();
        target.destroy();

        verify(executorService).shutdown();
    }

    @Test
    public void run_shouldCallExpectedMethods() {
        when(executorServiceHelper.createBoundedExecutorService()).thenReturn(executorService);
        when(threadLocalDelegateExecutorFactory.createExecutorService(executorService)).thenReturn(executorService);
        when(transactionTemplate.execute(any())).thenAnswer(args -> ((TransactionCallback) args.getArgument(0)).doInTransaction());
        when(executorService.submit((Runnable) any())).thenAnswer(args -> {
            ((Runnable) args.getArgument(0)).run();
            return null;
        });

        target.afterPropertiesSet();
        target.run(runnable);

        verify(runnable).run();
        verify(transactionTemplate).execute(any());
    }

    @Test
    public void run_shouldHandleErrorsGracefully() {
        when(executorServiceHelper.createBoundedExecutorService()).thenReturn(executorService);
        when(threadLocalDelegateExecutorFactory.createExecutorService(executorService)).thenReturn(executorService);
        when(transactionTemplate.execute(any())).thenAnswer(args -> ((TransactionCallback) args.getArgument(0)).doInTransaction());
        when(executorService.submit((Runnable) any())).thenAnswer(args -> {
            ((Runnable) args.getArgument(0)).run();
            return null;
        });
        doThrow(new RuntimeException("")).when(runnable).run();

        target.afterPropertiesSet();
        target.run(runnable);

        verify(runnable).run();
    }
}
