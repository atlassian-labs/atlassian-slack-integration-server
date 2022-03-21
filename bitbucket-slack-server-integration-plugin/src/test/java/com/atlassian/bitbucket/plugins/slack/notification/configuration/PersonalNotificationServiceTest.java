package com.atlassian.bitbucket.plugins.slack.notification.configuration;

import com.atlassian.bitbucket.commit.Commit;
import com.atlassian.bitbucket.commit.CommitDiscussion;
import com.atlassian.bitbucket.plugins.slack.model.ExtendedChannelToNotify;
import com.atlassian.bitbucket.plugins.slack.notification.renderer.SlackNotificationRenderer;
import com.atlassian.bitbucket.plugins.slack.util.TestUtil;
import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.bitbucket.pull.PullRequestParticipant;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.user.ApplicationUser;
import com.atlassian.bitbucket.user.SecurityService;
import com.atlassian.bitbucket.util.PageImpl;
import com.atlassian.bitbucket.util.PageRequestImpl;
import com.atlassian.bitbucket.watcher.Watcher;
import com.atlassian.bitbucket.watcher.WatcherService;
import com.atlassian.plugins.slack.api.SlackUser;
import com.atlassian.plugins.slack.api.notification.ChannelToNotify;
import com.atlassian.plugins.slack.settings.SlackSettingService;
import com.atlassian.plugins.slack.settings.SlackUserSettingsService;
import com.atlassian.plugins.slack.user.SlackUserManager;
import com.atlassian.sal.api.user.UserKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static com.atlassian.bitbucket.plugins.slack.notification.BitbucketPersonalNotificationTypes.COMMIT_AUTHOR_COMMENT;
import static com.atlassian.bitbucket.plugins.slack.notification.BitbucketPersonalNotificationTypes.PR_AUTHOR;
import static com.atlassian.bitbucket.plugins.slack.notification.BitbucketPersonalNotificationTypes.PR_REVIEWER_CREATED;
import static com.atlassian.bitbucket.plugins.slack.notification.BitbucketPersonalNotificationTypes.PR_REVIEWER_UPDATED;
import static com.atlassian.bitbucket.plugins.slack.notification.BitbucketPersonalNotificationTypes.PR_WATCHER;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PersonalNotificationServiceTest {
    public static final String COMMIT_ID = "commit11";
    public static final int COMMENT_AUTHOR_ID = 3;
    public static final String TEAM_ID = "someTeamId";
    public static final String SLACK_USER_ID = "someSlackUserId";

    @Mock
    SlackUserManager slackUserManager;
    @Mock
    SlackUserSettingsService slackUserSettingsService;
    @Mock
    SlackSettingService slackSettingService;
    @Mock
    SlackNotificationRenderer slackNotificationRenderer;
    @Mock
    WatcherService watcherService;
    @Mock
    SecurityService securityService;

    @Mock
    ApplicationUser currentUser;
    @Mock
    CommitDiscussion commitDiscussion;
    @Mock
    Repository repository;
    @Mock
    Commit commit;
    @Mock
    ApplicationUser commitAuthor;
    @Mock
    SlackUser slackUser;
    @Mock
    ApplicationUser authorUser;
    @Mock
    ApplicationUser reviewerUser;
    @Mock
    PullRequest pullRequest;
    @Mock
    PullRequestParticipant prAuthorParticipant;
    @Mock
    PullRequestParticipant prReviewerParticipant;
    @Mock
    Watcher watcher;
    @Mock
    ApplicationUser watcherUser;

    @InjectMocks
    PersonalNotificationService target;

    @Test
    void findNotificationsFor_shouldNotNotifyUserAboutHisOwnActionsOnHisCommit() {
        when(slackNotificationRenderer.findCommit(repository, COMMIT_ID)).thenReturn(commit);
        when(commit.getAuthor()).thenReturn(currentUser);
        when(commitDiscussion.getRepository()).thenReturn(repository);
        when(commitDiscussion.getCommitId()).thenReturn(COMMIT_ID);

        Set<ExtendedChannelToNotify> channels = target.findNotificationsFor(currentUser, commitDiscussion);

        assertThat(channels, empty());
    }

    @Test
    void findNotificationsFor_shouldNotifyUserAboutCommentOnHisCommit() {
        when(slackNotificationRenderer.findCommit(repository, COMMIT_ID)).thenReturn(commit);
        when(commit.getAuthor()).thenReturn(commitAuthor);
        when(commitAuthor.getId()).thenReturn(COMMENT_AUTHOR_ID);
        when(commitDiscussion.getRepository()).thenReturn(repository);
        when(commitDiscussion.getCommitId()).thenReturn(COMMIT_ID);
        String authorIdStr = String.valueOf(COMMENT_AUTHOR_ID);
        when(slackUserSettingsService.isPersonalNotificationTypeEnabled(new UserKey(authorIdStr),
                COMMIT_AUTHOR_COMMENT)).thenReturn(true);
        TestUtil.bypass(securityService, commitAuthor);
        when(slackUserSettingsService.getNotificationTeamId(new UserKey(authorIdStr))).thenReturn(TEAM_ID);
        when(slackUserManager.getByTeamIdAndUserKey(TEAM_ID, authorIdStr)).thenReturn(Optional.of(slackUser));
        when(slackUser.getUserToken()).thenReturn("someUserToken");
        when(slackUser.getSlackUserId()).thenReturn(SLACK_USER_ID);

        Set<ExtendedChannelToNotify> channels = target.findNotificationsFor(currentUser, commitDiscussion);

        assertThat(channels, contains(new ExtendedChannelToNotify(new ChannelToNotify(TEAM_ID, SLACK_USER_ID, null, true),
                "commit_author_comment", commitAuthor)));
    }

    @Test
    void findNotificationsFor_shouldNotifyPrAuthorOnPrUpdate() {
        when(pullRequest.getAuthor()).thenReturn(prAuthorParticipant);
        when(prAuthorParticipant.getUser()).thenReturn(authorUser);
        int authorId = 4;
        when(authorUser.getId()).thenReturn(authorId);

        String prAuthorIdStr = String.valueOf(authorId);
        when(slackUserSettingsService.isPersonalNotificationTypeEnabled(new UserKey(prAuthorIdStr),
                PR_AUTHOR)).thenReturn(true);
        TestUtil.bypass(securityService, authorUser);
        when(slackUserSettingsService.getNotificationTeamId(new UserKey(prAuthorIdStr))).thenReturn(TEAM_ID);
        when(slackUserManager.getByTeamIdAndUserKey(TEAM_ID, prAuthorIdStr)).thenReturn(Optional.of(slackUser));
        when(slackUser.getUserToken()).thenReturn("someUserToken");
        when(slackUser.getSlackUserId()).thenReturn(SLACK_USER_ID);
        when(watcherService.search(any(), any())).thenReturn(new PageImpl<>(new PageRequestImpl(0, 10), Collections.emptySet(), true));

        Set<ExtendedChannelToNotify> channels = target.findNotificationsFor(currentUser, pullRequest, Collections.emptySet());

        assertThat(channels, contains(new ExtendedChannelToNotify(new ChannelToNotify(TEAM_ID, SLACK_USER_ID, null, true),
                "pr_author", authorUser)));
    }

    @Test
    void findNotificationsFor_shouldNotifyPrWatcherOnPrUpdate() {
        when(pullRequest.getAuthor()).thenReturn(prAuthorParticipant);
        when(prAuthorParticipant.getUser()).thenReturn(authorUser);
        when(watcherService.search(any(), any()))
                .thenReturn(new PageImpl<>(new PageRequestImpl(0, 10), Collections.singleton(watcher), true));
        when(watcher.getUser()).thenReturn(watcherUser);
        int watcherId = 5;
        when(watcherUser.getId()).thenReturn(watcherId);

        String watcherIdStr = String.valueOf(watcherId);
        lenient().when(slackUserSettingsService.isPersonalNotificationTypeEnabled(new UserKey(watcherIdStr),
                PR_WATCHER)).thenReturn(true);
        TestUtil.bypass(securityService, null);
        when(slackUserSettingsService.getNotificationTeamId(new UserKey(watcherIdStr))).thenReturn(TEAM_ID);
        when(slackUserManager.getByTeamIdAndUserKey(TEAM_ID, watcherIdStr)).thenReturn(Optional.of(slackUser));
        when(slackUser.getUserToken()).thenReturn("someUserToken");
        when(slackUser.getSlackUserId()).thenReturn(SLACK_USER_ID);

        Set<ExtendedChannelToNotify> channels = target.findNotificationsFor(currentUser, pullRequest, Collections.emptySet());

        assertThat(channels, contains(new ExtendedChannelToNotify(new ChannelToNotify(TEAM_ID, SLACK_USER_ID, null, true),
                "pr_watcher", watcherUser)));
    }

    @Test
    void findNotificationsFor_shouldNotifyPrReviewerOnPrCreation() {
        when(pullRequest.getAuthor()).thenReturn(prAuthorParticipant);
        when(prAuthorParticipant.getUser()).thenReturn(authorUser);
        when(pullRequest.getReviewers()).thenReturn(Collections.singleton(prReviewerParticipant));
        when(prReviewerParticipant.getUser()).thenReturn(reviewerUser);
        when(watcherService.search(any(), any())).thenReturn(new PageImpl<>(new PageRequestImpl(0, 10), Collections.emptySet(), true));
        int reviewerId = 6;
        when(reviewerUser.getId()).thenReturn(reviewerId);

        String reviewerIdStr = String.valueOf(reviewerId);
        lenient().when(slackUserSettingsService.isPersonalNotificationTypeEnabled(new UserKey(reviewerIdStr),
                PR_REVIEWER_CREATED)).thenReturn(true);
        TestUtil.bypass(securityService, null);
        when(slackUserSettingsService.getNotificationTeamId(new UserKey(reviewerIdStr))).thenReturn(TEAM_ID);
        when(slackUserManager.getByTeamIdAndUserKey(TEAM_ID, reviewerIdStr)).thenReturn(Optional.of(slackUser));
        when(slackUser.getUserToken()).thenReturn("someUserToken");
        when(slackUser.getSlackUserId()).thenReturn(SLACK_USER_ID);

        Set<ExtendedChannelToNotify> channels = target.findNotificationsFor(currentUser, pullRequest, Collections.singleton(reviewerUser));

        assertThat(channels, contains(new ExtendedChannelToNotify(new ChannelToNotify(TEAM_ID, SLACK_USER_ID, null, true),
                "pr_reviewer_created", reviewerUser)));
    }

    @Test
    void findNotificationsFor_shouldNotifyPrReviewerOnPrUpdate() {
        when(pullRequest.getAuthor()).thenReturn(prAuthorParticipant);
        when(prAuthorParticipant.getUser()).thenReturn(authorUser);
        when(pullRequest.getReviewers()).thenReturn(Collections.singleton(prReviewerParticipant));
        when(prReviewerParticipant.getUser()).thenReturn(reviewerUser);
        when(watcherService.search(any(), any())).thenReturn(new PageImpl<>(new PageRequestImpl(0, 10), Collections.emptySet(), true));
        int reviewerId = 7;
        when(reviewerUser.getId()).thenReturn(reviewerId);

        String reviewerIdStr = String.valueOf(reviewerId);
        lenient().when(slackUserSettingsService.isPersonalNotificationTypeEnabled(new UserKey(reviewerIdStr),
                PR_REVIEWER_UPDATED)).thenReturn(true);
        TestUtil.bypass(securityService, null);
        when(slackUserSettingsService.getNotificationTeamId(new UserKey(reviewerIdStr))).thenReturn(TEAM_ID);
        when(slackUserManager.getByTeamIdAndUserKey(TEAM_ID, reviewerIdStr)).thenReturn(Optional.of(slackUser));
        when(slackUser.getUserToken()).thenReturn("someUserToken");
        when(slackUser.getSlackUserId()).thenReturn(SLACK_USER_ID);

        Set<ExtendedChannelToNotify> channels = target.findNotificationsFor(currentUser, pullRequest, Collections.emptySet());

        assertThat(channels, contains(new ExtendedChannelToNotify(new ChannelToNotify(TEAM_ID, SLACK_USER_ID, null, true),
                "pr_reviewer_updated", reviewerUser)));
    }
}
