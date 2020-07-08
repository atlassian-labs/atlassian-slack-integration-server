package com.atlassian.jira.plugins.slack.service.task.impl;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugins.slack.model.SlackIncomingMessage;
import com.atlassian.jira.plugins.slack.service.mentions.IssueMentionService;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.mockito.Mockito.verify;

public class ProcessIssueMentionTaskTest {
    @Mock
    private IssueMentionService issueMentionService;
    @Mock
    private Issue issue;
    @Mock
    private SlackIncomingMessage message;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private ProcessIssueMentionTask target;

    @Test
    public void call() {
        target.call();

        verify(issueMentionService).issueMentioned(issue, message);
    }
}
