package com.atlassian.confluence.plugins.slack.spacetochannel.notifications;

import com.atlassian.confluence.plugins.slack.spacetochannel.ao.AOEntityToChannelMapping;
import com.atlassian.confluence.plugins.slack.spacetochannel.ao.EntityToChannelMappingManager;
import com.atlassian.confluence.plugins.slack.spacetochannel.api.notifications.ConfluenceSlackEvent;
import com.atlassian.confluence.plugins.slack.spacetochannel.api.notifications.SpaceToChannelNotification;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.plugins.slack.api.notification.ChannelToNotify;
import com.atlassian.plugins.slack.api.notification.NotificationType;
import com.atlassian.plugins.slack.api.notification.SlackNotification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
public class SpaceNotificationContextTest {
    private static final String SPACE_KEY = "SPACE";
    private static final String TEAM_ID = "T";
    private static final String CHANNEL_ID = "C";

    @Mock
    private EntityToChannelMappingManager entityToChannelMappingManager;
    @Mock
    private ConfluenceSlackEvent event;
    @Mock
    private NotificationType notificationType;
    @Mock
    private SlackNotification<Object> slackNotification;
    @Mock
    private SpaceToChannelNotification spaceToChannelNotification;
    @Mock
    private Space space;
    @Mock
    private AOEntityToChannelMapping entity;

    @InjectMocks
    private SpaceNotificationContext<ConfluenceSlackEvent> target;

    @Test
    public void getChannels_shouldReturnEmptyIfNotificationOptionalIsEmpty() {
        when(notificationType.getNotification()).thenReturn(Optional.empty());

        List<ChannelToNotify> result = target.getChannels(event, notificationType);

        assertThat(result, hasSize(0));
    }

    @Test
    public void getChannels_shouldReturnEmptyIfSpaceIsNotFound() {
        when(notificationType.getNotification()).thenReturn(Optional.of(spaceToChannelNotification));
        when(spaceToChannelNotification.getSpace(event)).thenReturn(Optional.empty());

        List<ChannelToNotify> result = target.getChannels(event, notificationType);

        assertThat(result, hasSize(0));
    }

    @Test
    public void getChannels_shouldReturnEmptyIfNotificationTypeIsNotSpaceToChanelNotification() {
        when(notificationType.getNotification()).thenReturn(Optional.of(slackNotification));

        List<ChannelToNotify> result = target.getChannels(event, notificationType);

        assertThat(result, hasSize(0));
    }

    @Test
    public void getChannels_shouldReturnExpectedValue() {
        when(notificationType.getNotification()).thenReturn(Optional.of(spaceToChannelNotification));
        when(spaceToChannelNotification.getSpace(event)).thenReturn(Optional.of(space));
        when(space.getKey()).thenReturn(SPACE_KEY);
        when(entityToChannelMappingManager.getForEntityAndType(SPACE_KEY, notificationType))
                .thenReturn(Collections.singletonList(entity));
        when(entity.getTeamId()).thenReturn(TEAM_ID);
        when(entity.getChannelId()).thenReturn(CHANNEL_ID);

        List<ChannelToNotify> result = target.getChannels(event, notificationType);

        assertThat(result, hasSize(1));
        assertThat(result.get(0).getTeamId(), is(TEAM_ID));
        assertThat(result.get(0).getChannelId(), is(CHANNEL_ID));
    }
}
