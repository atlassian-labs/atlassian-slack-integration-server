package com.atlassian.jira.plugins.slack.service.notification;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.comments.Comment;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.issue.watchers.WatcherManager;
import com.atlassian.jira.plugins.slack.model.EventMatcherType;
import com.atlassian.jira.plugins.slack.model.event.JiraIssueEvent;
import com.atlassian.jira.plugins.slack.util.CommentUtil;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugins.slack.api.notification.Verbosity;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.plugins.slack.settings.SlackSettingService;
import com.atlassian.plugins.slack.settings.SlackUserSettingsService;
import com.atlassian.plugins.slack.user.SlackUserManager;
import com.atlassian.sal.api.user.UserKey;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.atlassian.jira.plugins.slack.model.JiraPersonalNotificationTypes.ASSIGNED;
import static com.atlassian.jira.plugins.slack.model.JiraPersonalNotificationTypes.WATCHER;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@Component
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class PersonalNotificationManager {
    private final SlackLinkManager slackLinkManager;
    private final SlackUserManager slackUserManager;
    private final SlackUserSettingsService slackUserSettingsService;
    private final WatcherManager watcherManager;
    private final SlackSettingService slackSettingService;
    private final CommentManager commentManager;

    public List<NotificationInfo> getNotificationsFor(final JiraIssueEvent event) {
        if (slackSettingService.isPersonalNotificationsDisabled()) {
            return Collections.emptyList();
        }

        final Issue issue = event.getIssue();
        final ApplicationUser actor = event.getEventAuthor().orElse(null);
        final EventMatcherType eventMatcher = event.getEventMatcher();
        final Comment comment = event.getComment().orElse(null);
        final boolean isRestrictedCommentAdded = eventMatcher == EventMatcherType.ISSUE_COMMENTED
                && CommentUtil.isRestricted(comment);

        // dedup notifications by user key
        final Map<String, NotificationInfo> userKeyToNotification = new HashMap<>();

        // assignee
        final ApplicationUser assignee = issue.getAssignee();
        final boolean isAssigneeActor = Objects.equals(assignee, actor);
        if (assignee != null && assignee.isActive() && !isAssigneeActor) {
            final boolean isAssigneeToBeNotified = slackUserSettingsService.isPersonalNotificationTypeEnabled(
                    new UserKey(assignee.getKey()), ASSIGNED);
            // if an assignee doesn't have access to the comment, they should not get a notification
            if (isAssigneeToBeNotified
                    && !(isRestrictedCommentAdded
                        && !userHasAccessToComment(issue, comment, assignee))) {
                addUserNotificationIfUserIsMapped(userKeyToNotification, assignee);
            }
        }

        // watchers
        watcherManager.getWatchersUnsorted(issue).stream()
                .filter(watcher -> Objects.nonNull(watcher))
                .filter(watcher -> watcher.isActive())
                .filter(watcher -> !Objects.equals(watcher, actor))
                .filter(watcher -> slackUserSettingsService.isPersonalNotificationTypeEnabled(new UserKey(watcher.getKey()), WATCHER))
                // if a watcher doesn't have access to the comment, they should not get a notification
                .filter(watcher -> !(isRestrictedCommentAdded && !userHasAccessToComment(issue, comment, watcher)))
                .forEach(watcher -> addUserNotificationIfUserIsMapped(userKeyToNotification, watcher));

        return new ArrayList<>(userKeyToNotification.values());
    }

    private void addUserNotificationIfUserIsMapped(final Map<String, NotificationInfo> userMap, final ApplicationUser applicationUser) {
        if (userMap.containsKey(applicationUser.getKey())) {
            return;
        }

        final String notificationTeamId = slackUserSettingsService.getNotificationTeamId(new UserKey(applicationUser.getKey()));
        if (isBlank(notificationTeamId)) {
            return;
        }

        slackUserManager.getByTeamIdAndUserKey(notificationTeamId, applicationUser.getKey())
                .filter(user -> isNotEmpty(user.getUserToken()))
                .map(user -> new NotificationInfo(
                        slackLinkManager.getLinkByTeamId(notificationTeamId).getOrNull(),
                        "",
                        user.getSlackUserId(),
                        null,
                        null,
                        "",
                        "",
                        "",
                        "",
                        true,
                        false,
                        Verbosity.EXTENDED
                ))
                .filter(info -> info.getLink() != null)
                .ifPresent(info -> userMap.put(applicationUser.getKey(), info));
    }

    private boolean userHasAccessToComment(final Issue issue, final Comment targetComment, final ApplicationUser user) {
        final List<Comment> comments = commentManager.getCommentsForUser(issue, user);
        return comments.stream()
                .anyMatch(comment -> Objects.equals(targetComment.getId(), comment.getId()));
    }
}
