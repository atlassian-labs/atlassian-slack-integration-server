package com.atlassian.plugins.slack.api.condition;

import com.atlassian.plugins.slack.user.SlackUserManager;
import com.atlassian.sal.api.user.UserKey;
import com.atlassian.sal.api.user.UserManager;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class UserLinkedWithSlackConditionTest {
    @Mock
    private SlackUserManager slackUserManager;
    @Mock
    private UserManager userManager;
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();
    @InjectMocks
    private UserLinkedWithSlackCondition condition;

    @Test
    public void shouldDisplay_shouldReturnExpectedValue() {
        UserKey userKey = UserKey.fromLong(1);
        when(userManager.getRemoteUserKey()).thenReturn(userKey);
        when(slackUserManager.getByUserKey(userKey)).thenReturn(Collections.singletonList(null));

        boolean result = condition.shouldDisplay(null);

        assertThat(result, is(true));
    }
}
