package com.atlassian.bitbucket.plugins.slack.rest;

import com.atlassian.bitbucket.permission.PermissionValidationService;
import com.atlassian.bitbucket.plugins.slack.notification.NotificationUtil;
import com.atlassian.bitbucket.plugins.slack.notification.configuration.NotificationConfigurationService;
import com.atlassian.bitbucket.plugins.slack.notification.configuration.NotificationDisableRequest;
import com.atlassian.bitbucket.plugins.slack.notification.configuration.NotificationEnableRequest;
import com.atlassian.bitbucket.plugins.slack.settings.BitbucketSlackSettingsService;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.repository.RepositoryService;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugins.slack.analytics.AnalyticsContextProvider;
import com.atlassian.plugins.slack.api.notification.Verbosity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.ws.rs.core.Response;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RepositoryToChannelConfigurationResourceTest {
    public static final int REPO_ID = 11;
    public static final String TEAM_ID = "someTeamId";
    public static final String CHANNEL_ID = "someChannelId";

    @Mock
    RepositoryService repositoryService;
    @Mock
    PermissionValidationService permissionValidationService;
    @Mock
    EventPublisher eventPublisher;
    @Mock
    NotificationConfigurationService notificationConfigurationService;
    @Mock
    BitbucketSlackSettingsService bitbucketSlackSettingsService;
    @Mock
    AnalyticsContextProvider analyticsContextProvider;

    @Mock
    Repository repository;

    @InjectMocks
    RepositoryToChannelConfigurationResource target;

    @Test
    void enableNotification() {
        when(repositoryService.getById(REPO_ID)).thenReturn(repository);

        Response response = target.enableNotification(REPO_ID, TEAM_ID, CHANNEL_ID, "CommitPushed", false);

        assertThat(response.getStatus(), is(200));
        verify(notificationConfigurationService).enable(new NotificationEnableRequest.Builder()
                .teamId(TEAM_ID)
                .channelId(CHANNEL_ID)
                .repository(repository)
                .notificationType("CommitPushed")
                .build());
    }

    @Test
    void enableNotification_whenInitialLink() {
        when(repositoryService.getById(REPO_ID)).thenReturn(repository);

        Response response = target.enableNotification(REPO_ID, TEAM_ID, CHANNEL_ID, null, true);

        assertThat(response.getStatus(), is(200));
        verify(notificationConfigurationService).enable(new NotificationEnableRequest.Builder()
                .teamId(TEAM_ID)
                .channelId(CHANNEL_ID)
                .repository(repository)
                .notificationTypes(NotificationUtil.ALL_NOTIFICATION_TYPE_KEYS)
                .build());
        verify(bitbucketSlackSettingsService).setVerbosity(REPO_ID, TEAM_ID, CHANNEL_ID, Verbosity.EXTENDED);
    }

    @Test
    void removeNotification() {
        when(repositoryService.getById(REPO_ID)).thenReturn(repository);

        Response response = target.removeNotification(REPO_ID, TEAM_ID, CHANNEL_ID, "CommitPushed");

        assertThat(response.getStatus(), is(200));
        verify(notificationConfigurationService).disable(new NotificationDisableRequest.Builder()
                .teamId(TEAM_ID)
                .channelId(CHANNEL_ID)
                .repository(repository)
                .notificationType("CommitPushed")
                .build());
    }

    @Test
    void removeNotificationsForChannel() {
        when(repositoryService.getById(REPO_ID)).thenReturn(repository);

        Response response = target.removeNotificationsForChannel(REPO_ID, TEAM_ID, CHANNEL_ID);

        assertThat(response.getStatus(), is(200));
        verify(notificationConfigurationService).disable(new NotificationDisableRequest.Builder()
                .teamId(TEAM_ID)
                .channelId(CHANNEL_ID)
                .repository(repository)
                .build());
        verify(bitbucketSlackSettingsService).clearVerbosity(REPO_ID, TEAM_ID, CHANNEL_ID);
    }

    @Test
    void saveOption() {
        when(repositoryService.getById(REPO_ID)).thenReturn(repository);

        Response response = target.saveOption(REPO_ID, TEAM_ID, CHANNEL_ID, "verbosity", "BASIC");

        assertThat(response.getStatus(), is(200));
        verify(bitbucketSlackSettingsService).setVerbosity(REPO_ID, TEAM_ID, CHANNEL_ID, Verbosity.BASIC);
    }
}
