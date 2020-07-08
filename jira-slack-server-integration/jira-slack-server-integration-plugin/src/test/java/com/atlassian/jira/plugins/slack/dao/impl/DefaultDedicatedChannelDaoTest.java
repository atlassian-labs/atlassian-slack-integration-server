package com.atlassian.jira.plugins.slack.dao.impl;

import com.atlassian.jira.plugins.slack.model.DedicatedChannel;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.atlassian.jira.plugins.slack.dao.impl.DefaultDedicatedChannelDao.DEDICATED_CHANNEL_PROPERTY_KEY;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultDedicatedChannelDaoTest {
    @Mock
    private DedicatedChannelStore dedicatedChannelStore;

    @Mock
    private DedicatedChannel dedicatedChannel;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private DefaultDedicatedChannelDao target;

    @Test
    public void insertDedicatedChannel() {
        when(dedicatedChannel.getIssueId()).thenReturn(1L);

        target.insertDedicatedChannel(dedicatedChannel);

        verify(dedicatedChannelStore).put(1L, DEDICATED_CHANNEL_PROPERTY_KEY, Optional.of(dedicatedChannel));
    }

    @Test
    public void getDedicatedChannel() {
        when(dedicatedChannelStore.get(1L, DEDICATED_CHANNEL_PROPERTY_KEY)).thenReturn(Optional.of(dedicatedChannel));

        Optional<DedicatedChannel> result = target.getDedicatedChannel(1L);

        assertThat(result, is(Optional.of(dedicatedChannel)));
    }

    @Test
    public void findMappingsForChannel() {
        List<DedicatedChannel> list = Collections.singletonList(dedicatedChannel);
        when(dedicatedChannelStore.getAllForChannel("C")).thenReturn(list);

        List<DedicatedChannel> result = target.findMappingsForChannel("C");

        assertThat(result, is(list));
    }

    @Test
    public void findMappingsByTeamId() {
        List<DedicatedChannel> list = Collections.singletonList(dedicatedChannel);
        when(dedicatedChannelStore.getAllByTeamId("T")).thenReturn(list);

        List<DedicatedChannel> result = target.findMappingsByTeamId("T");

        assertThat(result, is(list));
    }

    @Test
    public void deleteDedicatedChannel() {
        target.deleteDedicatedChannel(1L);

        verify(dedicatedChannelStore).delete(1L, DEDICATED_CHANNEL_PROPERTY_KEY);
    }
}
