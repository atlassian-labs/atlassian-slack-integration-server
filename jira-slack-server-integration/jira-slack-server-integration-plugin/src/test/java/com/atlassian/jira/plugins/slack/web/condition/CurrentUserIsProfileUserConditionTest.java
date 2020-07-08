package com.atlassian.jira.plugins.slack.web.condition;

import com.atlassian.jira.user.MockApplicationUser;
import com.google.common.collect.ImmutableMap;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CurrentUserIsProfileUserConditionTest {
    @InjectMocks
    CurrentUserIsProfileUserCondition target;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Test
    public void shouldDisplay_shouldReturnTrueForEqualUsers() {
        boolean shouldDisplay = target.shouldDisplay(ImmutableMap.of("user", new MockApplicationUser("currentUser name"),
                "profileUser", new MockApplicationUser("currentUser name")));

        assertTrue(shouldDisplay);
    }

    @Test
    public void shouldDisplay_shouldReturnFalseForDifferentUsers() {
        boolean shouldDisplay = target.shouldDisplay(ImmutableMap.of("user", new MockApplicationUser("currentUser name"),
                "profileUser", new MockApplicationUser("profileUser name")));

        assertFalse(shouldDisplay);
    }
}