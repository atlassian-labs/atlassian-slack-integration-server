package com.atlassian.jira.plugins.slack.service.notification;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.comments.Comment;
import com.atlassian.jira.issue.comments.CommentPermissionManager;
import com.atlassian.jira.issue.watchers.WatcherManager;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.plugins.slack.model.EventMatcherType;
import com.atlassian.jira.plugins.slack.model.JiraPersonalNotificationTypes;
import com.atlassian.jira.plugins.slack.model.event.JiraIssueEvent;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.SlackUser;
import com.atlassian.plugins.slack.api.notification.Verbosity;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.plugins.slack.settings.SlackSettingService;
import com.atlassian.plugins.slack.settings.SlackUserSettingsService;
import com.atlassian.plugins.slack.user.SlackUserManager;
import com.atlassian.sal.api.user.UserKey;
import io.atlassian.fugue.Either;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.atlassian.jira.plugins.slack.model.JiraPersonalNotificationTypes.ASSIGNED;
import static com.atlassian.jira.plugins.slack.model.JiraPersonalNotificationTypes.WATCHER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class PersonalNotificationManagerTest {
    @Mock
    private SlackLinkManager slackLinkManager;
    @Mock
    private SlackUserManager slackUserManager;
    @Mock
    private SlackUserSettingsService slackUserSettingsService;
    @Mock
    private WatcherManager watcherManager;
    @Mock
    private SlackSettingService slackSettingService;
    @Mock
    private CommentPermissionManager commentPermissionManager;
    @Mock
    private PermissionManager permissionManager;

    @Mock
    private JiraIssueEvent event;
    @Mock
    private Issue issue;
    @Mock
    private ApplicationUser assignee;
    @Mock
    private ApplicationUser eventAuthor;
    @Mock
    private Comment comment;

    @Mock
    private ApplicationUser user1WithConfiguredSlackNotification;
    @Mock
    private SlackUser slackUser1;
    @Mock
    private SlackLink slackLink1;
    private static final String USER1_NOTIFICATION_TEAM_ID = "user1-notification-team-id";
    @Mock
    private ApplicationUser user2WithConfiguredSlackNotification;
    @Mock
    private SlackUser slackUser2;
    @Mock
    private SlackLink slackLink2;
    private static final String USER2_NOTIFICATION_TEAM_ID = "user2-notification-team-id";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private PersonalNotificationManager target;

    @Before
    public void setUp() {
        when(slackSettingService.isPersonalNotificationsDisabled()).thenReturn(false);
        when(assignee.getKey()).thenReturn("assignee-key");
        when(assignee.isActive()).thenReturn(true);
        when(eventAuthor.getKey()).thenReturn("actor-key");
        when(event.getEventMatcher()).thenReturn(EventMatcherType.ISSUE_CREATED);
        when(event.getIssue()).thenReturn(issue);
        when(event.getComment()).thenReturn(Optional.empty());
        when(event.getEventAuthor()).thenReturn(Optional.of(eventAuthor));

        when(user1WithConfiguredSlackNotification.getKey()).thenReturn("user1-key");
        when(user1WithConfiguredSlackNotification.isActive()).thenReturn(true);
        when(slackUserSettingsService.getNotificationTeamId(new UserKey("user1-key"))).thenReturn(USER1_NOTIFICATION_TEAM_ID);
        when(slackUserManager.getByTeamIdAndUserKey(USER1_NOTIFICATION_TEAM_ID, "user1-key")).thenReturn(Optional.of(slackUser1));
        when(slackLinkManager.getLinkByTeamId(USER1_NOTIFICATION_TEAM_ID)).thenReturn(Either.right(slackLink1));
        when(slackUser1.getSlackUserId()).thenReturn("slackUser1Id");
        when(slackUser1.getUserToken()).thenReturn("slackToken1");

        when(user2WithConfiguredSlackNotification.getKey()).thenReturn("user2-key");
        when(user2WithConfiguredSlackNotification.isActive()).thenReturn(true);
        when(slackUserSettingsService.getNotificationTeamId(new UserKey("user2-key"))).thenReturn(USER2_NOTIFICATION_TEAM_ID);
        when(slackUserManager.getByTeamIdAndUserKey(USER2_NOTIFICATION_TEAM_ID, "user2-key")).thenReturn(Optional.of(slackUser2));
        when(slackLinkManager.getLinkByTeamId(USER2_NOTIFICATION_TEAM_ID)).thenReturn(Either.right(slackLink2));
        when(slackUser2.getUserToken()).thenReturn("slackToken2");
        when(slackUser2.getSlackUserId()).thenReturn("slackUser2Id");
    }

    @Test
    public void getNotificationsFor_shouldReturnEmptyList_whenPersonalNotificationsDisabled() {
        when(slackSettingService.isPersonalNotificationsDisabled()).thenReturn(true);

        List<NotificationInfo> result = target.getNotificationsFor(event);

        assertThat(result, empty());
        verifyNoInteractions(event);
    }

    @Test
    public void getNotificationsFor_shouldReturnNotificationForAssignee_happyPath() {
        // Arrange
        setupAssigneePersonalNotificationsForUser2AsAssignee();

        // Act
        List<NotificationInfo> result = target.getNotificationsFor(event);

        // Assert
        assertThat(result, contains(
                new NotificationInfo(
                        slackLink2,
                        "",
                        slackUser2.getSlackUserId(),
                        null,
                        null,
                        "",
                        "",
                        "",
                        "",
                        true,
                        false,
                        Verbosity.EXTENDED
                )
        ));
    }

    @Test
    public void getNotificationsFor_shouldReturnEmptyList_whenAssigneeIsNull() {
        // Arrange
        setupAssigneePersonalNotificationsForUser2AsAssignee();
        // break the happy path
        when(issue.getAssignee()).thenReturn(null);

        // Act
        List<NotificationInfo> result = target.getNotificationsFor(event);

        // Assert
        assertThat(result, empty());
    }

    @Test
    public void getNotificationsFor_shouldReturnEmptyList_whenAssigneeIsInactive() {
        // Arrange
        setupAssigneePersonalNotificationsForUser2AsAssignee();
        // break the happy path
        when(user2WithConfiguredSlackNotification.isActive()).thenReturn(false);

        // Act
        List<NotificationInfo> result = target.getNotificationsFor(event);

        // Assert
        assertThat(result, empty());
    }

    @Test
    public void getNotificationsFor_shouldNotNotifyAssignee_whenAssigneeIsActor() {
        // Arrange
        setupAssigneePersonalNotificationsForUser2AsAssignee();
        // break the happy path
        when(event.getEventAuthor()).thenReturn(Optional.of(user2WithConfiguredSlackNotification));

        // Act
        List<NotificationInfo> result = target.getNotificationsFor(event);

        // Assert
        assertThat(result, empty());
    }

    @Test
    public void getNotificationsFor_shouldNotNotifyAssignee_whenAssigneeNotificationDisabled() {
        // Arrange
        setupAssigneePersonalNotificationsForUser2AsAssignee();
        // break the happy path
        setPersonalNotificationSettingFor(user2WithConfiguredSlackNotification.getKey(), ASSIGNED, false);

        // Act
        List<NotificationInfo> result = target.getNotificationsFor(event);

        // Assert
        assertThat(result, empty());
    }

    @Test
    public void getNotificationsFor_shouldNotNotifyAssignee_whenUserDoesNotHaveAccessToIssue() {
        // Arrange
        setupAssigneePersonalNotificationsForUser2AsAssignee();
        // break the happy path
        setBrowseProjectsPermissionFor(issue, user2WithConfiguredSlackNotification, false);

        // Act
        List<NotificationInfo> result = target.getNotificationsFor(event);

        // Assert
        assertThat(result, empty());
    }

    @Test
    public void getNotificationsFor_shouldNotNotifyAssignee_whenNoSlackTeamIdConfigured() {
        // Arrange
        setupAssigneePersonalNotificationsForUser2AsAssignee();
        // break the happy path
        when(slackUserSettingsService.getNotificationTeamId(new UserKey(user2WithConfiguredSlackNotification.getKey()))).thenReturn(null);

        // Act
        List<NotificationInfo> result = target.getNotificationsFor(event);

        // Assert
        assertThat(result, empty());
    }

    @Test
    public void getNotificationsFor_shouldNotNotifyAssignee_whenSlackUserNotFound() {
        // Arrange
        setupAssigneePersonalNotificationsForUser2AsAssignee();
        // break the happy path
        when(slackUserManager.getByTeamIdAndUserKey(USER2_NOTIFICATION_TEAM_ID, user2WithConfiguredSlackNotification.getKey())).thenReturn(Optional.empty());

        // Act
        List<NotificationInfo> result = target.getNotificationsFor(event);

        // Assert
        assertThat(result, empty());
    }

    @Test
    public void getNotificationsFor_shouldNotNotifyAssignee_whenUserTokenNotConfigured() {
        // Arrange
        setupAssigneePersonalNotificationsForUser2AsAssignee();
        // break the happy path
        when(slackUser2.getUserToken()).thenReturn(null);

        // Act
        List<NotificationInfo> result = target.getNotificationsFor(event);

        // Assert
        assertThat(result, empty());
    }

    @Test
    public void getNotificationsFor_shouldNotNotifyAssignee_whenSlackLinkNotFound() {
        // Arrange
        setupAssigneePersonalNotificationsForUser2AsAssignee();
        // break the happy path
        when(slackLinkManager.getLinkByTeamId(USER2_NOTIFICATION_TEAM_ID)).thenReturn(Either.left(new Exception("Exception!")));

        // Act
        List<NotificationInfo> result = target.getNotificationsFor(event);

        // Assert
        assertThat(result, empty());
    }

    @Test
    public void getNotificationsFor_shouldReturnNotificationsForWatchers_happyPath() {
        // Arrange
        setupWatcherPersonalNotificationsForUser1AndUser2();

        // Act
        List<NotificationInfo> result = target.getNotificationsFor(event);

        // Assert
        assertThat(result, containsInAnyOrder(
                new NotificationInfo(
                        slackLink1,
                        "",
                        slackUser1.getSlackUserId(),
                        null,
                        null,
                        "",
                        "",
                        "",
                        "",
                        true,
                        false,
                        Verbosity.EXTENDED
                ),
                new NotificationInfo(
                        slackLink2,
                        "",
                        slackUser2.getSlackUserId(),
                        null,
                        null,
                        "",
                        "",
                        "",
                        "",
                        true,
                        false,
                        Verbosity.EXTENDED
                )
        ));
    }

    @Test
    public void getNotificationsFor_shouldNotNotifyWatcher_whenWatcherIsActor() {
        // Arrange
        setupWatcherPersonalNotificationsForUser1AndUser2();
        when(event.getEventAuthor()).thenReturn(Optional.of(user2WithConfiguredSlackNotification));
        // Act
        List<NotificationInfo> result = target.getNotificationsFor(event);

        // Assert
        assertThat(result, containsInAnyOrder(
                new NotificationInfo(
                        slackLink1,
                        "",
                        slackUser1.getSlackUserId(),
                        null,
                        null,
                        "",
                        "",
                        "",
                        "",
                        true,
                        false,
                        Verbosity.EXTENDED
                )
        ));
    }

    @Test
    public void getNotificationsFor_shouldNotNotifyWatcher_whenWatcherIsInactive() {
        // Arrange
        setupWatcherPersonalNotificationsForUser1AndUser2();
        when(user2WithConfiguredSlackNotification.isActive()).thenReturn(false);

        // Act
        List<NotificationInfo> result = target.getNotificationsFor(event);

        // Assert
        assertThat(result, containsInAnyOrder(
                new NotificationInfo(
                        slackLink1,
                        "",
                        slackUser1.getSlackUserId(),
                        null,
                        null,
                        "",
                        "",
                        "",
                        "",
                        true,
                        false,
                        Verbosity.EXTENDED
                )
        ));
    }

    @Test
    public void getNotificationsFor_shouldNotNotifyWatcher_whenWatcherNotificationDisabled() {
        // Arrange
        setupWatcherPersonalNotificationsForUser1AndUser2();
        setPersonalNotificationSettingFor(user2WithConfiguredSlackNotification.getKey(), WATCHER, false);

        // Act
        List<NotificationInfo> result = target.getNotificationsFor(event);

        // Assert
        assertThat(result, containsInAnyOrder(
                new NotificationInfo(
                        slackLink1,
                        "",
                        slackUser1.getSlackUserId(),
                        null,
                        null,
                        "",
                        "",
                        "",
                        "",
                        true,
                        false,
                        Verbosity.EXTENDED
                )
        ));
    }

    @Test
    public void getNotificationsFor_shouldNotNotifyWatcher_whenWatcherDoesNotHaveAccessToIssue() {
        // Arrange
        setupWatcherPersonalNotificationsForUser1AndUser2();
        setBrowseProjectsPermissionFor(issue, user2WithConfiguredSlackNotification, false);

        // Act
        List<NotificationInfo> result = target.getNotificationsFor(event);

        // Assert
        assertThat(result, containsInAnyOrder(
                new NotificationInfo(
                        slackLink1,
                        "",
                        slackUser1.getSlackUserId(),
                        null,
                        null,
                        "",
                        "",
                        "",
                        "",
                        true,
                        false,
                        Verbosity.EXTENDED
                )
        ));
    }

    @Test
    public void getNotificationsFor_shouldNotNotifyAssigneeWithRestrictedComment_whenUserDoesNotHaveAccessToComment() {
        /// Arrange
        setupWatcherPersonalNotificationsForUser1AndUser2();
        makeEventAsIssueCommentedWithRestrictedComment();
        overwriteCommentPermissionForAllCommentsForSpecificUser(comment, user1WithConfiguredSlackNotification, true);
        overwriteCommentPermissionForAllCommentsForSpecificUser(comment, user2WithConfiguredSlackNotification, false);

        // Act
        List<NotificationInfo> result = target.getNotificationsFor(event);

        // Assert
        assertThat(result, containsInAnyOrder(
                new NotificationInfo(
                        slackLink1,
                        "",
                        slackUser1.getSlackUserId(),
                        null,
                        null,
                        "",
                        "",
                        "",
                        "",
                        true,
                        false,
                        Verbosity.EXTENDED
                )
        ));
    }

    @Test
    public void getNotificationsFor_shouldDedupNotifications_whenAssigneeAndWatcherAreSame() {
        // Arrange
        setupAssigneePersonalNotificationsForUser2AsAssignee();
        setupWatcherPersonalNotificationsForUser1AndUser2();

        // Act
        List<NotificationInfo> result = target.getNotificationsFor(event);

        // Assert
        assertThat(result, containsInAnyOrder(
                new NotificationInfo(
                        slackLink1,
                        "",
                        slackUser1.getSlackUserId(),
                        null,
                        null,
                        "",
                        "",
                        "",
                        "",
                        true,
                        false,
                        Verbosity.EXTENDED
                ),
                new NotificationInfo(
                        slackLink2,
                        "",
                        slackUser2.getSlackUserId(),
                        null,
                        null,
                        "",
                        "",
                        "",
                        "",
                        true,
                        false,
                        Verbosity.EXTENDED
                )
        ));
        //ensure both assignee/watchers have been executed
        verify(slackUserSettingsService).isPersonalNotificationTypeEnabled(
                new UserKey(user2WithConfiguredSlackNotification.getKey()), ASSIGNED);
        verify(slackUserSettingsService).isPersonalNotificationTypeEnabled(
                new UserKey(user1WithConfiguredSlackNotification.getKey()), WATCHER);
        verify(slackUserSettingsService).isPersonalNotificationTypeEnabled(
                new UserKey(user2WithConfiguredSlackNotification.getKey()), WATCHER);
        verify(slackUserSettingsService, times(2)).getNotificationTeamId(any(UserKey.class));
        verifyNoMoreInteractions(slackUserSettingsService);
    }

    @Test
    public void getNotificationsFor_shouldFilterOutNullWatchers() {
        // Arrange
        setupWatcherPersonalNotificationsForUser1AndUser2();
        when(watcherManager.getWatchersUnsorted(issue)).thenReturn(
                Arrays.asList(user1WithConfiguredSlackNotification, null, user2WithConfiguredSlackNotification)
        );

        // Act
        List<NotificationInfo> result = target.getNotificationsFor(event);

        // Assert
        assertThat(result, containsInAnyOrder(
                new NotificationInfo(
                        slackLink1,
                        "",
                        slackUser1.getSlackUserId(),
                        null,
                        null,
                        "",
                        "",
                        "",
                        "",
                        true,
                        false,
                        Verbosity.EXTENDED
                ),
                new NotificationInfo(
                        slackLink2,
                        "",
                        slackUser2.getSlackUserId(),
                        null,
                        null,
                        "",
                        "",
                        "",
                        "",
                        true,
                        false,
                        Verbosity.EXTENDED
                )
        ));
    }

    @Test
    public void getNotificationsFor_shouldSupportEmptyEventAuthor() {
        // Arrange
        setupAssigneePersonalNotificationsForUser2AsAssignee();
        setupWatcherPersonalNotificationsForUser1AndUser2();
        when(event.getEventAuthor()).thenReturn(Optional.empty());

        // Act
        List<NotificationInfo> result = target.getNotificationsFor(event);

        // Assert
        assertThat(result, containsInAnyOrder(
                new NotificationInfo(
                        slackLink1,
                        "",
                        slackUser1.getSlackUserId(),
                        null,
                        null,
                        "",
                        "",
                        "",
                        "",
                        true,
                        false,
                        Verbosity.EXTENDED
                ),
                new NotificationInfo(
                        slackLink2,
                        "",
                        slackUser2.getSlackUserId(),
                        null,
                        null,
                        "",
                        "",
                        "",
                        "",
                        true,
                        false,
                        Verbosity.EXTENDED
                )
        ));
        //ensure both assignee/watchers have been executed
        verify(slackUserSettingsService).isPersonalNotificationTypeEnabled(
                new UserKey(user2WithConfiguredSlackNotification.getKey()), ASSIGNED);
        verify(slackUserSettingsService).isPersonalNotificationTypeEnabled(
                new UserKey(user1WithConfiguredSlackNotification.getKey()), WATCHER);
        verify(slackUserSettingsService).isPersonalNotificationTypeEnabled(
                new UserKey(user2WithConfiguredSlackNotification.getKey()), WATCHER);
        verify(slackUserSettingsService, times(2)).getNotificationTeamId(any(UserKey.class));
        verifyNoMoreInteractions(slackUserSettingsService);
    }

    // ======= Helper methods =======

    private void setBrowseProjectsPermissionFor(Issue issue, ApplicationUser user, boolean hasPermission) {
        when(permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, issue, user))
                .thenReturn(hasPermission);
    }

    private void setPersonalNotificationSettingFor(String userKey, JiraPersonalNotificationTypes personalNotificationType, boolean enabled) {
        when(slackUserSettingsService.isPersonalNotificationTypeEnabled(new UserKey(userKey), personalNotificationType))
                .thenReturn(enabled);
    }

    private void setupAssigneePersonalNotificationsForUser2AsAssignee() {
        when(issue.getAssignee()).thenReturn(user2WithConfiguredSlackNotification);
        setPersonalNotificationSettingFor(user2WithConfiguredSlackNotification.getKey(), ASSIGNED, true);
        setBrowseProjectsPermissionFor(issue, user2WithConfiguredSlackNotification, true);
    }

    private void setupWatcherPersonalNotificationsForUser1AndUser2() {
        when(watcherManager.getWatchersUnsorted(issue)).thenReturn(
                Arrays.asList(user1WithConfiguredSlackNotification, user2WithConfiguredSlackNotification)
        );
        setPersonalNotificationSettingFor(user1WithConfiguredSlackNotification.getKey(), WATCHER, true);
        setPersonalNotificationSettingFor(user2WithConfiguredSlackNotification.getKey(), WATCHER, true);
        setBrowseProjectsPermissionFor(issue, user1WithConfiguredSlackNotification, true);
        setBrowseProjectsPermissionFor(issue, user2WithConfiguredSlackNotification, true);
    }

    private void makeEventAsIssueCommentedWithRestrictedComment() {
        when(event.getEventMatcher()).thenReturn(EventMatcherType.ISSUE_COMMENTED);
        when(event.getComment()).thenReturn(Optional.of(comment));
        when(comment.getIssue()).thenReturn(issue);
        when(comment.getGroupLevel()).thenReturn("restricted!");
    }
    
    private void overwriteCommentPermissionForAllCommentsForSpecificUser(Comment comment, ApplicationUser user, boolean hasPermission) {
        when(commentPermissionManager.hasBrowsePermission(user, comment)).thenReturn(hasPermission);
    }
}
