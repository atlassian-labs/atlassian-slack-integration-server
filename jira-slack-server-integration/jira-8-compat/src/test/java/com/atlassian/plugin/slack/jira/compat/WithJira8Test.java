package com.atlassian.plugin.slack.jira.compat;

import com.atlassian.jira.util.BuildUtilsInfoImpl;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Optional;
import java.util.concurrent.Callable;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@PrepareForTest(WithJira8.class)
@PowerMockIgnore("javax.*")
@RunWith(PowerMockRunner.class)
public class WithJira8Test {
    @Mock
    private BuildUtilsInfoImpl buildUtilsInfo;
    @Mock
    private Callable<Object> callable;
    @Mock
    private Object object;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Test
    public void isJira8OrGreater_shouldReturnFalseForJira7() throws Exception {
        mockJiraVersion(7);

        boolean result = WithJira8.isJira8OrGreater();

        assertThat(result, is(false));
    }

    @Test
    public void isJira8OrGreater_shouldReturnTrueForJira8() throws Exception {
        mockJiraVersion(8);

        boolean result = WithJira8.isJira8OrGreater();

        assertThat(result, is(true));
    }

    private void mockJiraVersion(int version) throws Exception {
        PowerMockito.whenNew(BuildUtilsInfoImpl.class)
                .withAnyArguments()
                .thenReturn(buildUtilsInfo);
        when(buildUtilsInfo.getVersionNumbers()).thenReturn(new int[]{version});
    }

    @Test
    public void withJira8_shouldReturnFalseForJira7() throws Exception {
        mockJiraVersion(7);

        Optional<Object> result = WithJira8.withJira8(callable);

        assertThat(result.isPresent(), is(false));
        verify(callable, never()).call();
    }

    @Test
    public void withJira8_shouldReturnTrueForJira8() throws Exception {
        mockJiraVersion(8);
        when(callable.call()).thenReturn(object);

        Optional<Object> result = WithJira8.withJira8(callable);

        assertThat(result.isPresent(), is(true));
        assertThat(result.get(), sameInstance(object));
        verify(callable).call();
    }
}
