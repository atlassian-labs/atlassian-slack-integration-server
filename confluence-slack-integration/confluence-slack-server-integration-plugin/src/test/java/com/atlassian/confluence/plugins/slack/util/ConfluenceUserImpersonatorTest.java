package com.atlassian.confluence.plugins.slack.util;

import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.function.Supplier;

import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ConfluenceUserImpersonatorTest {
    @Mock
    ConfluenceUser previousConfluenceUser;
    @Mock
    ConfluenceUser confluenceUser;
    @Mock
    Supplier action;

    private MockedStatic<AuthenticatedUserThreadLocal> mockedThreadLocal;

    @InjectMocks
    ConfluenceUserImpersonator target;

    @After
    public void tearDown() {
        if (mockedThreadLocal != null) {
            mockedThreadLocal.close();
        }
    }

    @Test
    public void impersonateShouldSetActiveUser() {
        mockedThreadLocal = mockStatic(AuthenticatedUserThreadLocal.class);
        mockedThreadLocal.when(AuthenticatedUserThreadLocal::get).thenReturn(previousConfluenceUser);

        target.impersonate(confluenceUser, action, "some cause");

        verify(action).get();
        mockedThreadLocal.verify(() -> AuthenticatedUserThreadLocal.set(confluenceUser));
        mockedThreadLocal.verify(() -> AuthenticatedUserThreadLocal.set(previousConfluenceUser));
    }
}
