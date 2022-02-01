package com.atlassian.jira.plugins.slack.service.task.impl;

import com.atlassian.jira.plugins.slack.model.SlackDeletedMessage;
import com.atlassian.jira.plugins.slack.service.mentions.IssueMentionService;
import com.atlassian.plugins.slack.api.ConversationKey;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProcessMessageDeletedTaskTest {
    @Mock
    private IssueMentionService issueMentionService;
    @Mock
    private SlackDeletedMessage message;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private ProcessMessageDeletedTask target;

    @Test
    public void call() {
        when(message.getChannelId()).thenReturn("C");
        when(message.getTeamId()).thenReturn("T");
        when(message.getTs()).thenReturn("ts");

        target.call();

        verify(issueMentionService).deleteMessageMention(new ConversationKey("T", "C"), "ts");
    }
}
