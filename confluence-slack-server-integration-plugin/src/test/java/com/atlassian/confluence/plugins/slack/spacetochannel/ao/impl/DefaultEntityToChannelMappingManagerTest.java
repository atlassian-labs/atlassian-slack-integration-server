package com.atlassian.confluence.plugins.slack.spacetochannel.ao.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.confluence.plugins.slack.spacetochannel.ao.AOEntityToChannelMapping;
import com.atlassian.confluence.plugins.slack.spacetochannel.ao.DefaultEntityToChannelMappingManager;
import com.atlassian.plugins.slack.api.notification.NotificationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DefaultEntityToChannelMappingManagerTest {
    private static final String CHANNEL = "C";
    private static final String ENTITY = "E";
    private static final String TEAM = "T";
    private static final String NOTIFICATION_TYPE_KEY = "K";

    @Mock
    private ActiveObjects activeObjects;
    @Mock
    private AOEntityToChannelMapping entity1;
    @Mock
    private NotificationType notificationType;

    @InjectMocks
    private DefaultEntityToChannelMappingManager target;

    @Test
    public void getAll_shouldReturnProperResult() {
        when(activeObjects.find(AOEntityToChannelMapping.class)).thenReturn(new AOEntityToChannelMapping[]{entity1});

        final List<AOEntityToChannelMapping> result = target.getAll();

        assertThat(result, contains(entity1));
    }

    @Test
    public void getForChannel_shouldReturnProperResult() {
        when(activeObjects.find(AOEntityToChannelMapping.class, "CHANNEL_ID = ?", CHANNEL))
                .thenReturn(new AOEntityToChannelMapping[]{entity1});

        final List<AOEntityToChannelMapping> result = target.getForChannel(CHANNEL);

        assertThat(result, contains(entity1));
    }

    @Test
    public void hasConfigurationForChannel_shouldReturnProperResult() {
        when(activeObjects.count(AOEntityToChannelMapping.class, "CHANNEL_ID = ?", CHANNEL)).thenReturn(1);

        boolean result = target.hasConfigurationForChannel(CHANNEL);

        assertThat(result, is(true));
    }

    @Test
    public void countForChannel_shouldReturnProperResult() {
        when(activeObjects.count(AOEntityToChannelMapping.class, "CHANNEL_ID = ?", CHANNEL)).thenReturn(1);

        int result = target.countForChannel(CHANNEL);

        assertThat(result, is(1));
    }

    @Test
    public void getForEntity_shouldReturnProperResult() {
        when(activeObjects.find(AOEntityToChannelMapping.class, "ENTITY_KEY = ?", ENTITY))
                .thenReturn(new AOEntityToChannelMapping[]{entity1});

        final List<AOEntityToChannelMapping> result = target.getForEntity(ENTITY);

        assertThat(result, contains(entity1));
    }

    @Test
    public void hasConfigurationForEntity_shouldReturnProperResult() {
        when(activeObjects.count(AOEntityToChannelMapping.class, "ENTITY_KEY = ?", ENTITY)).thenReturn(1);

        boolean result = target.hasConfigurationForEntity(ENTITY);

        assertThat(result, is(true));
    }

    @Test
    public void hasConfigurationForEntityChannelAndTypey_shouldReturnProperResult() {
        when(activeObjects.count(
                AOEntityToChannelMapping.class,
                "ENTITY_KEY = ? AND CHANNEL_ID = ? AND MESSAGE_TYPE_KEY = ?",
                ENTITY,
                CHANNEL,
                NOTIFICATION_TYPE_KEY)
        ).thenReturn(1);
        when(notificationType.getKey()).thenReturn(NOTIFICATION_TYPE_KEY);

        boolean result = target.hasConfigurationForEntityChannelAndType(ENTITY, CHANNEL, notificationType);

        assertThat(result, is(true));
    }

    @Test
    public void getForEntityAndChannel_shouldReturnProperResult() {
        when(activeObjects.find(AOEntityToChannelMapping.class, "ENTITY_KEY = ? AND CHANNEL_ID = ?", ENTITY, CHANNEL))
                .thenReturn(new AOEntityToChannelMapping[]{entity1});

        final List<AOEntityToChannelMapping> result = target.getForEntityAndChannel(ENTITY, CHANNEL);

        assertThat(result, contains(entity1));
    }

    @Test
    public void getForEntityAndType_shouldReturnProperResult() {
        when(activeObjects.find(AOEntityToChannelMapping.class, "ENTITY_KEY = ? AND MESSAGE_TYPE_KEY = ?", ENTITY, NOTIFICATION_TYPE_KEY))
                .thenReturn(new AOEntityToChannelMapping[]{entity1});
        when(notificationType.getKey()).thenReturn(NOTIFICATION_TYPE_KEY);

        final List<AOEntityToChannelMapping> result = target.getForEntityAndType(ENTITY, notificationType);

        assertThat(result, contains(entity1));
    }

    @Test
    public void addNotificationForEntityAndChannel_shouldCreateEntity() {
        String owner = "O";
        when(activeObjects.create(AOEntityToChannelMapping.class)).thenReturn(entity1);
        when(notificationType.getKey()).thenReturn(NOTIFICATION_TYPE_KEY);

        target.addNotificationForEntityAndChannel(
                ENTITY, owner, TEAM, CHANNEL, notificationType);

        verify(entity1).setEntityKey(ENTITY);
        verify(entity1).setOwner(owner);
        verify(entity1).setTeamId(TEAM);
        verify(entity1).setChannelId(CHANNEL);
        verify(entity1).setMessageTypeKey(NOTIFICATION_TYPE_KEY);
        verify(entity1).save();


    }

    @Test
    public void removeNotificationsForEntity_shouldRemoveEntity() {
        target.removeNotificationsForEntity(ENTITY);
        verify(activeObjects).deleteWithSQL(AOEntityToChannelMapping.class, "ENTITY_KEY = ?", ENTITY);
    }

    @Test
    public void removeNotificationsForEntityAndChannel_shouldRemoveEntities() {
        target.removeNotificationsForEntityAndChannel(ENTITY, CHANNEL);
        verify(activeObjects).deleteWithSQL(
                AOEntityToChannelMapping.class, "ENTITY_KEY = ? AND CHANNEL_ID = ?", ENTITY, CHANNEL);
    }

    @Test
    public void removeNotificationsForTeam_shouldRemoveEntities() {
        target.removeNotificationsForTeam(TEAM);
        verify(activeObjects).deleteWithSQL(AOEntityToChannelMapping.class, "TEAM_ID = ?", TEAM);
    }

    @Test
    public void removeNotificationForEntityAndChannel_shouldRemoveEntities() {
        when(notificationType.getKey()).thenReturn(NOTIFICATION_TYPE_KEY);

        target.removeNotificationForEntityAndChannel(ENTITY, CHANNEL, notificationType);

        verify(activeObjects).deleteWithSQL(
                AOEntityToChannelMapping.class,
                "ENTITY_KEY = ? AND CHANNEL_ID = ? AND MESSAGE_TYPE_KEY = ?",
                ENTITY,
                CHANNEL,
                NOTIFICATION_TYPE_KEY);
    }

    @Test
    public void removeNotificationsForChannel_shouldRemoveEntities() {
        String channel = "C";
        target.removeNotificationsForChannel(channel);
        verify(activeObjects).deleteWithSQL(AOEntityToChannelMapping.class, "CHANNEL_ID = ?", channel);
    }

}
