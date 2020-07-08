package com.atlassian.jira.plugins.slack.dao.impl;

import com.atlassian.jira.plugins.slack.model.DedicatedChannel;
import com.atlassian.jira.plugins.slack.storage.json.JsonStore;
import com.atlassian.jira.plugins.slack.storage.json.JsonStoreFactory;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.atlassian.jira.plugins.slack.dao.impl.DedicatedChannelStore.SLACK_DEDICATED_CHANNEL_ENTITY_NAME;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DedicatedChannelStoreTest {
    @Mock
    private JsonStoreFactory jsonStoreFactory;

    @Mock
    private JsonStore<DedicatedChannel> jsonStore;
    @Mock
    private DedicatedChannel dedicatedChannel1;
    @Mock
    private DedicatedChannel dedicatedChannel2;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private DedicatedChannelStore target;

    @Test
    public void getEntityTypeAndConstructorCreatesStore() {
        when(jsonStoreFactory.getJsonStore(SLACK_DEDICATED_CHANNEL_ENTITY_NAME, DedicatedChannel.class)).thenReturn(jsonStore);
        target = new DedicatedChannelStore(jsonStoreFactory);

        Class<DedicatedChannel> result = target.getEntityType();

        assertThat(result, sameInstance(DedicatedChannel.class));
        verify(jsonStoreFactory).getJsonStore(SLACK_DEDICATED_CHANNEL_ENTITY_NAME, DedicatedChannel.class);
    }

    @Test
    public void getAllForChannel() {
        List<DedicatedChannel> list = Arrays.asList(dedicatedChannel1, dedicatedChannel2);
        when(dedicatedChannel1.getChannelId()).thenReturn("C1");
        when(dedicatedChannel2.getChannelId()).thenReturn("C2");

        when(jsonStoreFactory.getJsonStore(SLACK_DEDICATED_CHANNEL_ENTITY_NAME, DedicatedChannel.class)).thenReturn(jsonStore);
        when(jsonStore.findByPredicate(any())).thenAnswer(args -> list.stream()
                .filter(v -> ((Predicate<DedicatedChannel>) args.getArgument(0)).test(v))
                .collect(Collectors.toList()));
        target = new DedicatedChannelStore(jsonStoreFactory);

        List<DedicatedChannel> result = target.getAllForChannel("C1");

        assertThat(result, contains(dedicatedChannel1));
    }

    @Test
    public void getAllByTeamId() {
        List<DedicatedChannel> list = Arrays.asList(dedicatedChannel1, dedicatedChannel2);
        when(dedicatedChannel1.getTeamId()).thenReturn("T1");
        when(dedicatedChannel2.getTeamId()).thenReturn("T2");

        when(jsonStoreFactory.getJsonStore(SLACK_DEDICATED_CHANNEL_ENTITY_NAME, DedicatedChannel.class)).thenReturn(jsonStore);
        when(jsonStore.findByPredicate(any())).thenAnswer(args -> list.stream()
                .filter(v -> ((Predicate<DedicatedChannel>) args.getArgument(0)).test(v))
                .collect(Collectors.toList()));
        target = new DedicatedChannelStore(jsonStoreFactory);

        List<DedicatedChannel> result = target.getAllByTeamId("T2");

        assertThat(result, contains(dedicatedChannel2));
    }
}
