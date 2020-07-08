package com.atlassian.plugin.slack.jira.compat;

import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.event.operation.SpanningOperation;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Optional;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class Jira8IssueEventWrapperTest {
    @Mock
    private IssueEvent issueEvent;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Test
    public void isSpanningOperation_shouldReturnFalse_whenThereIsNoSpanningOperation() {
        when(issueEvent.getSpanningOperation()).thenReturn(Optional.empty());

        boolean result = Jira8IssueEventWrapper.isSpanningOperation(issueEvent);

        assertThat(result, Matchers.is(false));
    }

    @Test
    public void isSpanningOperation_shouldReturnTrue_whenThereIsASpanningOperation() {
        when(issueEvent.getSpanningOperation()).thenReturn(Optional.of(SpanningOperation.builder().type("").id("").build()));

        boolean result = Jira8IssueEventWrapper.isSpanningOperation(issueEvent);

        assertThat(result, Matchers.is(true));
    }
}
