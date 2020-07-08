package com.atlassian.confluence.plugins.slack.util;

import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.function.Supplier;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.*")
@PrepareForTest(AuthenticatedUserThreadLocal.class)
public class ConfluenceUserImpersonatorTest {
    @Mock
    ConfluenceUser previousConfluenceUser;
    @Mock
    ConfluenceUser confluenceUser;
    @Mock
    Supplier action;

    @InjectMocks
    ConfluenceUserImpersonator target;

    @Test
    public void impersonateShouldSetActiveUser() {
        PowerMockito.mockStatic(AuthenticatedUserThreadLocal.class);
        when(AuthenticatedUserThreadLocal.get()).thenReturn(previousConfluenceUser);

        target.impersonate(confluenceUser, action, "some cause");

        verify(action).get();
        PowerMockito.verifyStatic(AuthenticatedUserThreadLocal.class);
        AuthenticatedUserThreadLocal.set(confluenceUser);
        PowerMockito.verifyStatic(AuthenticatedUserThreadLocal.class);
        AuthenticatedUserThreadLocal.set(previousConfluenceUser);
    }
}
