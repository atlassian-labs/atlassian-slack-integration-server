package com.atlassian.confluence.plugins.slack.spacetochannel.rest;

import com.atlassian.confluence.plugins.slack.spacetochannel.configuration.SpaceToChannelSettings;
import com.atlassian.confluence.plugins.slack.spacetochannel.events.analytic.SpaceToChannelLinkedAnalyticEvent;
import com.atlassian.confluence.plugins.slack.spacetochannel.events.analytic.SpaceToChannelNotificationDisabledAnalyticEvent;
import com.atlassian.confluence.plugins.slack.spacetochannel.events.analytic.SpaceToChannelNotificationEnabledAnalyticEvent;
import com.atlassian.confluence.plugins.slack.spacetochannel.notifications.SpaceNotificationContext;
import com.atlassian.confluence.plugins.slack.spacetochannel.service.SlackSpaceToChannelService;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.spaces.SpaceManager;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugins.slack.analytics.AnalyticsContextProvider;
import com.atlassian.plugins.slack.api.descriptor.NotificationTypeService;
import com.atlassian.plugins.slack.api.notification.NotificationType;
import com.atlassian.sal.api.user.UserKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.Optional;

import static com.atlassian.confluence.security.Permission.ADMINISTER;
import static com.atlassian.confluence.security.PermissionManager.TARGET_APPLICATION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SpaceToChannelConfigurationResourceTest {
    private static final String TEAM_ID = "someTeamId";
    private static final String CHANNEL_ID = "someChannelId";
    private static final String SPACE_KEY = "SPACE";
    private static final String NOTIF_NAME = "NOT1";
    private static final String USER = "USR";
    private static final UserKey userKey = new UserKey(USER);

    @Mock
    private SpaceManager spaceManager;
    @Mock(lenient = true)
    private PermissionManager permissionManager;
    @Mock
    private SlackSpaceToChannelService slackSpaceToChannelService;
    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private NotificationTypeService notificationTypeService;
    @Mock
    private AnalyticsContextProvider analyticsContextProvider;

    @Mock
    private Space space;
    @Mock
    private ConfluenceUser user;
    @Mock
    private NotificationType notificationType;
    @Mock
    private SpaceToChannelSettings spaceToChannelSettings;

    @InjectMocks
    private SpaceToChannelConfigurationResource target;

    @BeforeEach
    public void setUp() {
        AuthenticatedUserThreadLocal.set(user);
        lenient().when(user.getKey()).thenReturn(userKey);
    }

    @AfterEach
    public void tearDown() {
        AuthenticatedUserThreadLocal.reset();
    }

    @Test
    public void enableNotification_shouldReturnForbiddenWhenUserIsNotAdmin() {
        when(spaceManager.getSpace(SPACE_KEY)).thenReturn(space);

        Response result = target.enableNotification(SPACE_KEY, TEAM_ID, CHANNEL_ID, NOTIF_NAME, true);

        assertThat(result.getStatus(), is(403));
    }

    @Test
    public void enableNotification_shouldReturnBadRequestWhenNotificationTypeIsInvalid() {
        when(spaceManager.getSpace(SPACE_KEY)).thenReturn(space);
        when(permissionManager.hasPermission(user, ADMINISTER, space)).thenReturn(true);
        when(notificationTypeService.getNotificationTypeForKey(NOTIF_NAME)).thenReturn(Optional.empty());

        Response result = target.enableNotification(SPACE_KEY, TEAM_ID, CHANNEL_ID, NOTIF_NAME, true);

        assertThat(result.getStatus(), is(400));
    }

    @Test
    public void enableNotification_shouldReturnOkAndDoNothingIfUserIsSpaceAdminAndMappingExists() {
        when(spaceManager.getSpace(SPACE_KEY)).thenReturn(space);
        when(permissionManager.hasPermission(user, ADMINISTER, space)).thenReturn(true);
        when(notificationTypeService.getNotificationTypeForKey(NOTIF_NAME)).thenReturn(Optional.of(notificationType));
        when(slackSpaceToChannelService.hasMappingForEntityChannelAndType(SPACE_KEY, CHANNEL_ID, notificationType)).thenReturn(true);

        Response result = target.enableNotification(SPACE_KEY, TEAM_ID, CHANNEL_ID, NOTIF_NAME, true);

        assertThat(result.getStatus(), is(200));
    }

    @Test
    public void enableNotification_shouldAddNotificationIfUserIsInstanceAdmin() {
        when(spaceManager.getSpace(SPACE_KEY)).thenReturn(space);
        when(permissionManager.hasPermission(user, ADMINISTER, TARGET_APPLICATION)).thenReturn(true);
        when(notificationTypeService.getNotificationTypeForKey(NOTIF_NAME)).thenReturn(Optional.of(notificationType));
        when(slackSpaceToChannelService.hasMappingForEntityChannelAndType(SPACE_KEY, CHANNEL_ID, notificationType)).thenReturn(false);
        when(user.getKey()).thenReturn(userKey);

        Response result = target.enableNotification(SPACE_KEY, TEAM_ID, CHANNEL_ID, NOTIF_NAME, true);

        assertThat(result.getStatus(), is(200));
        verify(slackSpaceToChannelService).addNotificationForSpaceAndChannel(
                SPACE_KEY, USER, TEAM_ID, CHANNEL_ID, notificationType);
        verify(eventPublisher).publish(isA(SpaceToChannelLinkedAnalyticEvent.class));
        verify(eventPublisher).publish(isA(SpaceToChannelNotificationEnabledAnalyticEvent.class));
    }

    @Test
    public void removeNotification_shouldReturnForbiddenWhenUserIsNotAdmin() {
        when(spaceManager.getSpace(SPACE_KEY)).thenReturn(space);

        Response result = target.removeNotification(SPACE_KEY, TEAM_ID, CHANNEL_ID, NOTIF_NAME);

        assertThat(result.getStatus(), is(403));
    }

    @Test
    public void removeNotification_shouldReturnBadRequestWhenNotificationTypeIsInvalid() {
        when(spaceManager.getSpace(SPACE_KEY)).thenReturn(space);
        when(permissionManager.hasPermission(user, ADMINISTER, space)).thenReturn(true);
        when(notificationTypeService.getNotificationTypeForKey(NOTIF_NAME)).thenReturn(Optional.empty());

        Response result = target.removeNotification(SPACE_KEY, TEAM_ID, CHANNEL_ID, NOTIF_NAME);

        assertThat(result.getStatus(), is(400));
    }

    @Test
    public void removeNotification_shouldReturnOkAndDoNothingIfUserIsSpaceAdminAndMappingDoesNotExist() {
        when(spaceManager.getSpace(SPACE_KEY)).thenReturn(space);
        when(permissionManager.hasPermission(user, ADMINISTER, space)).thenReturn(true);
        when(notificationTypeService.getNotificationTypeForKey(NOTIF_NAME)).thenReturn(Optional.of(notificationType));
        when(slackSpaceToChannelService.getSpaceToChannelSettings(SPACE_KEY, CHANNEL_ID)).thenReturn(Optional.empty());

        Response result = target.removeNotification(SPACE_KEY, TEAM_ID, CHANNEL_ID, NOTIF_NAME);

        assertThat(result.getStatus(), is(200));
    }

    @Test
    public void removeNotification_shouldReturnOkAndDoNothingIfUserIsSpaceAdminAndNotifTypeIsNotEnabled() {
        when(spaceManager.getSpace(SPACE_KEY)).thenReturn(space);
        when(permissionManager.hasPermission(user, ADMINISTER, space)).thenReturn(true);
        when(notificationTypeService.getNotificationTypeForKey(NOTIF_NAME)).thenReturn(Optional.of(notificationType));
        when(slackSpaceToChannelService.getSpaceToChannelSettings(SPACE_KEY, CHANNEL_ID)).thenReturn(Optional.of(spaceToChannelSettings));
        when(spaceToChannelSettings.getNotificationTypes()).thenReturn(Collections.emptySet());

        Response result = target.removeNotification(SPACE_KEY, TEAM_ID, CHANNEL_ID, NOTIF_NAME);

        assertThat(result.getStatus(), is(200));
    }

    @Test
    public void removeNotification_shouldRemoveNotificationIfUserIsInstanceAdmin() {
        when(spaceManager.getSpace(SPACE_KEY)).thenReturn(space);
        when(permissionManager.hasPermission(user, ADMINISTER, TARGET_APPLICATION)).thenReturn(true);
        when(notificationTypeService.getNotificationTypeForKey(NOTIF_NAME)).thenReturn(Optional.of(notificationType));
        when(slackSpaceToChannelService.getSpaceToChannelSettings(SPACE_KEY, CHANNEL_ID)).thenReturn(Optional.of(spaceToChannelSettings));
        when(spaceToChannelSettings.getNotificationTypes()).thenReturn(Collections.singleton(notificationType));

        Response result = target.removeNotification(SPACE_KEY, TEAM_ID, CHANNEL_ID, NOTIF_NAME);

        assertThat(result.getStatus(), is(200));
        verify(slackSpaceToChannelService).removeNotificationForSpaceAndChannel(
                SPACE_KEY, CHANNEL_ID, notificationType);
    }

    @Test
    public void removeNotificationsForChannel_shouldReturnForbiddenWhenUserIsNotAdmin() {
        when(spaceManager.getSpace(SPACE_KEY)).thenReturn(space);

        Response result = target.removeNotificationsForChannel(SPACE_KEY, TEAM_ID, CHANNEL_ID);

        assertThat(result.getStatus(), is(403));
    }

    @Test
    public void removeNotificationsForChannel_shouldDoNothingNotificationTypesAreNotFound() {
        when(spaceManager.getSpace(SPACE_KEY)).thenReturn(space);
        when(permissionManager.hasPermission(user, ADMINISTER, space)).thenReturn(true);

        Response result = target.removeNotificationsForChannel(SPACE_KEY, TEAM_ID, CHANNEL_ID);

        assertThat(result.getStatus(), is(200));
        verify(slackSpaceToChannelService, never()).removeNotificationsForSpaceAndChannel(any(), any());
    }

    @Test
    public void removeNotificationsForChannel_shouldRemoveNotificationIfUserIsInstanceAdmin() {
        when(spaceManager.getSpace(SPACE_KEY)).thenReturn(space);
        when(permissionManager.hasPermission(user, ADMINISTER, TARGET_APPLICATION)).thenReturn(true);
        when(notificationTypeService.getNotificationTypes(SpaceNotificationContext.KEY)).thenReturn(Collections.singletonList(notificationType));
        when(slackSpaceToChannelService.getSpaceToChannelSettings(SPACE_KEY, CHANNEL_ID)).thenReturn(Optional.of(spaceToChannelSettings));
        when(spaceToChannelSettings.getNotificationTypes()).thenReturn(Collections.singleton(notificationType));

        Response result = target.removeNotificationsForChannel(SPACE_KEY, TEAM_ID, CHANNEL_ID);

        assertThat(result.getStatus(), is(200));
        verify(slackSpaceToChannelService).removeNotificationsForSpaceAndChannel(SPACE_KEY, CHANNEL_ID);
        verify(eventPublisher).publish(isA(SpaceToChannelNotificationDisabledAnalyticEvent.class));
    }
}
