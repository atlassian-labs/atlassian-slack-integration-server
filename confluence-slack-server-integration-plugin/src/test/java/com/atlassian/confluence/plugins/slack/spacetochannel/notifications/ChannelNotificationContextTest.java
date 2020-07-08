package com.atlassian.confluence.plugins.slack.spacetochannel.notifications;

import com.atlassian.confluence.plugins.slack.spacetochannel.ao.AOEntityToChannelMapping;
import com.atlassian.confluence.plugins.slack.spacetochannel.api.notifications.ConfluenceSlackEvent;
import com.atlassian.confluence.plugins.slack.spacetochannel.api.notifications.SpaceToChannelNotification;
import com.atlassian.confluence.plugins.slack.spacetochannel.model.ChannelContext;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.plugins.slack.api.notification.BaseSlackEvent;
import com.atlassian.plugins.slack.api.notification.ChannelToNotify;
import com.atlassian.plugins.slack.api.notification.NotificationType;
import com.atlassian.plugins.slack.api.notification.SlackNotification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ChannelNotificationContextTest {
    private static final String TEAM_ID = "T";
    private static final String CHANNEL_ID = "C";

    @Mock
    private ChannelContext event;
    @Mock
    private NotificationType notificationType;
    @Mock
    private SlackNotification<BaseSlackEvent> slackNotification;
    @Mock
    private SpaceToChannelNotification<ConfluenceSlackEvent> spaceToChannelNotification;
    @Mock
    private Space space;
    @Mock
    private AOEntityToChannelMapping entity;

    @InjectMocks
    private ChannelNotificationContext<ChannelContext> target;

    @Test
    public void getChannels_shouldReturnEmptyIfNotificationOptionalIsEmpty() {
        when(event.getTeamId()).thenReturn(TEAM_ID);
        when(event.getChannelId()).thenReturn(CHANNEL_ID);
        when(event.getThreadTs()).thenReturn("ts");

        List<ChannelToNotify> result = target.getChannels(event, notificationType);

        assertThat(result, hasSize(1));
        assertThat(result.get(0).getTeamId(), is(TEAM_ID));
        assertThat(result.get(0).getChannelId(), is(CHANNEL_ID));
        assertThat(result.get(0).getThreadTs(), is("ts"));
    }
}
