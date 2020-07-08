package com.atlassian.confluence.plugins.slack.spacetochannel.listener;

import com.atlassian.confluence.event.events.space.SpaceRemoveEvent;
import com.atlassian.confluence.plugins.slack.spacetochannel.service.SlackSpaceToChannelService;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.event.api.EventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SlackSpaceEventListenerTest {
    private static final String SPACE_KEY = "S";

    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private SlackSpaceToChannelService spaceToChannelService;
    @Mock
    private SpaceRemoveEvent spaceRemoveEvent;
    @Mock
    private Space space;

    @InjectMocks
    private SlackSpaceEventListener target;

    @Test
    public void removeNotificationsForSpace_shouldReturnExpectedValue() {
        when(spaceRemoveEvent.getSpace()).thenReturn(space);
        when(space.getKey()).thenReturn(SPACE_KEY);

        target.spaceRemovedEvent(spaceRemoveEvent);

        verify(spaceToChannelService).removeNotificationsForSpace(SPACE_KEY);
    }
}
