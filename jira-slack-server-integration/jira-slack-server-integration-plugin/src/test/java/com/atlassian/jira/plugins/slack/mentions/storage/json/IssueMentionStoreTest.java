package com.atlassian.jira.plugins.slack.mentions.storage.json;

import com.atlassian.jira.plugins.slack.model.mentions.IssueMention;
import com.atlassian.jira.plugins.slack.storage.json.JsonStore;
import com.atlassian.jira.plugins.slack.storage.json.JsonStoreFactory;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class IssueMentionStoreTest {
    @Mock
    private JsonStoreFactory jsonStoreFactory;

    @Mock
    private JsonStore<IssueMention> jsonStore;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Test
    public void getEntityType() {
        when(jsonStoreFactory.getJsonStore(IssueMentionStore.SLACK_ISSUE_MENTIONS_ENTITY_NAME, IssueMention.class))
                .thenReturn(jsonStore);

        IssueMentionStore target = new IssueMentionStore(jsonStoreFactory);
        assertThat(target.getEntityType(), sameInstance(IssueMention.class));
    }
}
