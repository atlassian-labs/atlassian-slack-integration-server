package com.atlassian.confluence.plugins.slack.util;

import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.function.Supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ConfluenceUserImpersonatorTest {
    @Mock
    ConfluenceUser previousConfluenceUser;
    @Mock
    ConfluenceUser confluenceUser;

    @InjectMocks
    ConfluenceUserImpersonator target;

    @Test
    public void impersonateShouldSetActiveUser() {
        AuthenticatedUserThreadLocal.set(previousConfluenceUser);

        Supplier<Void> action = Mockito.mock();
        doAnswer(invocation -> {
            assertThat(AuthenticatedUserThreadLocal.get(), Matchers.equalTo(confluenceUser));
            return null;
        }).when(action).get();

        target.impersonate(confluenceUser, action, "some cause");

        verify(action).get();

        assertThat(AuthenticatedUserThreadLocal.get(), Matchers.equalTo(previousConfluenceUser));
    }
}
