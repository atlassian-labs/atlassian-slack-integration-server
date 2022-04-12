package com.atlassian.bitbucket.plugins.slack.notification.configuration;

import com.atlassian.bitbucket.AuthorisationException;
import com.atlassian.bitbucket.permission.Permission;
import com.atlassian.bitbucket.permission.PermissionValidationService;
import com.atlassian.bitbucket.plugins.slack.event.analytic.RepositoryNotificationDisabledAnalyticEvent;
import com.atlassian.bitbucket.plugins.slack.event.analytic.RepositoryNotificationEnabledAnalyticEvent;
import com.atlassian.bitbucket.plugins.slack.notification.configuration.dao.NotificationConfigurationDao;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugins.slack.analytics.AnalyticsContext;
import com.atlassian.plugins.slack.analytics.AnalyticsContextProvider;
import com.atlassian.plugins.slack.api.ConversationKey;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DefaultNotificationConfigurationServiceTest {
    @Mock
    private NotificationConfigurationDao dao;
    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private PermissionValidationService permissionService;
    @Mock
    private AnalyticsContextProvider analyticsContextProvider;
    @Mock
    private AnalyticsContext analyticsContext;

    @Mock
    private Repository repository;

    @InjectMocks
    private DefaultNotificationConfigurationService target;

    @Test
    public void disable_shouldPerformExpectedAction() {
        final NotificationDisableRequest request = new NotificationDisableRequest.Builder().repository(repository).build();

        target.disable(request);

        verify(permissionService).validateForRepository(repository, Permission.REPO_ADMIN);
        verify(dao).delete(request);
        verify(eventPublisher, times(32)).publish(any(RepositoryNotificationDisabledAnalyticEvent.class));
    }

    @Test
    public void enable_shouldFailIfNoPermission() {
        final NotificationDisableRequest request = new NotificationDisableRequest.Builder().repository(repository).build();

        doThrow(AuthorisationException.class).when(permissionService)
                .validateForRepository(repository, Permission.REPO_ADMIN);

        Assertions.assertThrows(AuthorisationException.class, () -> target.disable(request));
    }

    @Test
    public void enable_shouldPerformExpectedAction() {
        final NotificationEnableRequest request = new NotificationEnableRequest.Builder()
                .repository(repository)
                .notificationType("N")
                .channelId("C")
                .teamId("T")
                .build();

        target.enable(request);

        verify(permissionService).validateForRepository(repository, Permission.REPO_ADMIN);
        verify(dao).create(request);
        verify(eventPublisher).publish(any(RepositoryNotificationEnabledAnalyticEvent.class));
    }

    @Test
    public void removeNotificationsForTeam_shouldPerformExpectedDeletion() {
        target.removeNotificationsForTeam("T");

        verify(dao).removeNotificationsForTeam("T");
    }

    @Test
    public void removeNotificationsForChannel_shouldPerformExpectedDeletion() {
        target.removeNotificationsForChannel(new ConversationKey("T", "C"));

        verify(dao).removeNotificationsForChannel(new ConversationKey("T", "C"));
    }
}
