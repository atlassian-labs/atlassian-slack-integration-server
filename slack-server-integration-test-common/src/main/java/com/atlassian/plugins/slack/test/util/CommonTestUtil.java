package com.atlassian.plugins.slack.test.util;

import com.atlassian.plugins.slack.util.AsyncExecutor;
import lombok.experimental.UtilityClass;
import org.mockito.stubbing.Answer;

import java.util.concurrent.TimeUnit;

import static org.mockito.AdditionalAnswers.answer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;

@UtilityClass
public class CommonTestUtil {
    private static final Answer<Object> runRunnableAnswer = answer((Runnable r) -> {
        r.run();
        return null;
    });

    public static void bypass(final AsyncExecutor asyncExecutor) {
        doAnswer(runRunnableAnswer).when(asyncExecutor).run(any(Runnable.class));
    }

    public static void bypassDelayed(final AsyncExecutor asyncExecutor) {
        doAnswer(runRunnableAnswer).when(asyncExecutor).runDelayed(any(Runnable.class), anyLong(), any(TimeUnit.class));
    }
}
