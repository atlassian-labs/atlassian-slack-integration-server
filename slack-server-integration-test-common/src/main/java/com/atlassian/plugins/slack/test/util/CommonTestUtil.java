package com.atlassian.plugins.slack.test.util;

import com.atlassian.plugins.slack.util.AsyncExecutor;
import lombok.experimental.UtilityClass;

import static org.mockito.AdditionalAnswers.answer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@UtilityClass
public class CommonTestUtil {
    public static void bypass(final AsyncExecutor asyncExecutor) {
        doAnswer(answer((Runnable r) -> {
            r.run();
            return null;
        })).when(asyncExecutor).run(any(Runnable.class));
    }
}
