package com.atlassian.bitbucket.plugins.slack.notification.configuration;

import com.atlassian.bitbucket.commit.Commit;
import com.atlassian.bitbucket.commit.CommitDiscussion;
import com.atlassian.bitbucket.commit.NoSuchCommitException;
import com.atlassian.bitbucket.plugins.slack.model.ExtendedChannelToNotify;
import com.atlassian.bitbucket.plugins.slack.notification.BitbucketPersonalNotificationTypes;
import com.atlassian.bitbucket.plugins.slack.notification.renderer.SlackNotificationRenderer;
import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.bitbucket.pull.PullRequestParticipant;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.user.ApplicationUser;
import com.atlassian.bitbucket.user.SecurityService;
import com.atlassian.bitbucket.util.Page;
import com.atlassian.bitbucket.util.PageUtils;
import com.atlassian.bitbucket.watcher.Watcher;
import com.atlassian.bitbucket.watcher.WatcherSearchRequest;
import com.atlassian.bitbucket.watcher.WatcherService;
import com.atlassian.plugins.slack.api.notification.ChannelToNotify;
import com.atlassian.plugins.slack.settings.SlackSettingService;
import com.atlassian.plugins.slack.settings.SlackUserSettingsService;
import com.atlassian.plugins.slack.user.SlackUserManager;
import com.atlassian.sal.api.user.UserKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.atlassian.bitbucket.plugins.slack.notification.BitbucketPersonalNotificationTypes.COMMIT_AUTHOR_COMMENT;
import static com.atlassian.bitbucket.plugins.slack.notification.BitbucketPersonalNotificationTypes.PR_AUTHOR;
import static com.atlassian.bitbucket.plugins.slack.notification.BitbucketPersonalNotificationTypes.PR_REVIEWER_CREATED;
import static com.atlassian.bitbucket.plugins.slack.notification.BitbucketPersonalNotificationTypes.PR_REVIEWER_UPDATED;
import static com.atlassian.bitbucket.plugins.slack.notification.BitbucketPersonalNotificationTypes.PR_WATCHER;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@Component
public class PersonalNotificationService {
    private static final Integer MAX_WATCHERS = Integer.getInteger("slack.notification.max.watchers", 30);

    private final SlackUserManager slackUserManager;
    private final SlackUserSettingsService slackUserSettingsService;
    private final SlackSettingService slackSettingService;
    private final SlackNotificationRenderer slackNotificationRenderer;
    private final WatcherService watcherService;
    private final SecurityService securityService;

    @Autowired
    public PersonalNotificationService(
            final SlackUserManager slackUserManager,
            final SlackUserSettingsService slackUserSettingsService,
            final SlackSettingService slackSettingService,
            final SlackNotificationRenderer slackNotificationRenderer,
            final WatcherService watcherService,
            final SecurityService securityService) {
        this.slackUserManager = slackUserManager;
        this.slackUserSettingsService = slackUserSettingsService;
        this.slackSettingService = slackSettingService;
        this.slackNotificationRenderer = slackNotificationRenderer;
        this.watcherService = watcherService;
        this.securityService = securityService;
    }

    public Set<ExtendedChannelToNotify> findNotificationsFor(final ApplicationUser currentUser,
                                                             final CommitDiscussion commitDiscussion) {
        if (slackSettingService.isPersonalNotificationsDisabled()) {
            return Collections.emptySet();
        }

        final Map<Integer, ExtendedChannelToNotify> userMap = new HashMap<>();

        // author
        final Repository repository = commitDiscussion.getRepository();
        try {
            final Commit commit = slackNotificationRenderer.findCommit(repository, commitDiscussion.getCommitId());
            if (commit.getAuthor() instanceof ApplicationUser) {
                final ApplicationUser author = (ApplicationUser) commit.getAuthor();
                addAuthorNotification(currentUser, userMap, author, COMMIT_AUTHOR_COMMENT);
            }
        } catch (NoSuchCommitException e) {
            // no-op
        }

        return new HashSet<>(userMap.values());
    }

    public Set<ExtendedChannelToNotify> findNotificationsFor(final ApplicationUser currentUser,
                                                             final PullRequest pullRequest,
                                                             final Set<ApplicationUser> addedReviewers) {
        if (slackSettingService.isPersonalNotificationsDisabled()) {
            return Collections.emptySet();
        }

        final Map<Integer, ExtendedChannelToNotify> usersToNotify = new HashMap<>();

        // author
        final ApplicationUser author = pullRequest.getAuthor().getUser();
        addAuthorNotification(currentUser, usersToNotify, author, PR_AUTHOR);

        // watchers
        final Set<Watcher> watchers = getWatchers(pullRequest);
        watchers.stream()
                .filter(watcher -> !Objects.equals(watcher.getUser(), currentUser) && !Objects.equals(watcher.getUser(), author))
                .filter(watcher -> isPersonalNotificationTypeEnabled(watcher.getUser(), PR_WATCHER))
                .forEach(watcher -> addUserChannelToNotify(usersToNotify, watcher.getUser(), PR_WATCHER));

        // reviewers
        Set<ApplicationUser> reviewersWithoutActor = pullRequest.getReviewers().stream()
                .map(PullRequestParticipant::getUser)
                .filter(user -> !Objects.equals(user, currentUser))
                .collect(Collectors.toSet());
        reviewersWithoutActor.stream()
                .filter(user -> isPersonalNotificationTypeEnabled(user, PR_REVIEWER_UPDATED))
                .forEach(user -> addUserChannelToNotify(usersToNotify, user, PR_REVIEWER_UPDATED));
        reviewersWithoutActor.stream()
                .filter(user -> {
                    final boolean notifyMyWhenIAmAddedToPr = isPersonalNotificationTypeEnabled(user, PR_REVIEWER_CREATED);
                    final boolean userWasJustAdded = addedReviewers.contains(user);
                    return notifyMyWhenIAmAddedToPr && userWasJustAdded;
                })
                .forEach(user -> addUserChannelToNotify(usersToNotify, user, PR_REVIEWER_CREATED));

        return new HashSet<>(usersToNotify.values());
    }

    private void addAuthorNotification(final ApplicationUser currentUser,
                                       final Map<Integer, ExtendedChannelToNotify> userMap,
                                       final ApplicationUser author,
                                       final BitbucketPersonalNotificationTypes type) {
        final boolean isAuthorCurrentActor = Objects.equals(author, currentUser);
        if (!isAuthorCurrentActor) {
            final boolean isAssigneeToBeNotified = isPersonalNotificationTypeEnabled(author, type);
            if (isAssigneeToBeNotified) {
                addUserChannelToNotify(userMap, author, type);
            }
        }
    }

    private void addUserChannelToNotify(final Map<Integer, ExtendedChannelToNotify> usersToNotify,
                                        final ApplicationUser applicationUser,
                                        final BitbucketPersonalNotificationTypes type) {
        int userId = applicationUser.getId();
        if (usersToNotify.containsKey(userId)) {
            return;
        }

        final String userIdString = String.valueOf(userId);
        final String notificationTeamId = securityService
                .impersonating(applicationUser, "Slack plugin impersonates user to get access to user settings")
                .call(() -> slackUserSettingsService.getNotificationTeamId(new UserKey(userIdString)));
        if (isBlank(notificationTeamId)) {
            return;
        }

        slackUserManager.getByTeamIdAndUserKey(notificationTeamId, userIdString)
                .filter(user -> isNotEmpty(user.getUserToken()))
                .map(user -> new ExtendedChannelToNotify(new ChannelToNotify(
                        notificationTeamId,
                        user.getSlackUserId(),
                        null,
                        true), type.name().toLowerCase(), applicationUser))
                .ifPresent(info -> usersToNotify.put(userId, info));
    }

    private Set<Watcher> getWatchers(final PullRequest pullRequest) {
        final Page<Watcher> result = watcherService.search(
                new WatcherSearchRequest.Builder(pullRequest).build(),
                PageUtils.newRequest(0, MAX_WATCHERS));
        final Set<Watcher> response = new HashSet<>();
        result.getValues().forEach(response::add);
        return response;
    }

    // impersonate user. Otherwise AccessDeniedException is thrown on attempt to execute
    // userSettingsService.getUserSettings(userKey)
    private boolean isPersonalNotificationTypeEnabled(final ApplicationUser user, final Enum key) {
        return securityService
                .impersonating(user, "Slack plugin impersonates user to get access to user settings")
                .call(() -> slackUserSettingsService.isPersonalNotificationTypeEnabled(
                        new UserKey(String.valueOf(user.getId())), key));
    }
}
