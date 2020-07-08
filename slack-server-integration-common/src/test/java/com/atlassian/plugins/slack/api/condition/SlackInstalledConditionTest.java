package com.atlassian.plugins.slack.api.condition;

import com.atlassian.plugins.slack.link.SlackLinkManager;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class SlackInstalledConditionTest {
    @Mock
    private SlackLinkManager slackLinkManager;
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();
    @InjectMocks
    private SlackInstalledCondition condition;

    @Test
    public void shouldDisplay_shouldReturnExpectedValue() {
        when(slackLinkManager.isAnyLinkDefined()).thenReturn(true);

        boolean actualFlag = condition.shouldDisplay(null);

        assertThat(actualFlag, is(true));
    }
}
