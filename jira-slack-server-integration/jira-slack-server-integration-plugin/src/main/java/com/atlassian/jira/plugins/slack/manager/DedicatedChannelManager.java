package com.atlassian.jira.plugins.slack.manager;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugins.slack.model.DedicatedChannel;
import com.atlassian.jira.plugins.slack.model.event.JiraIssueEvent;
import com.atlassian.jira.plugins.slack.service.notification.NotificationInfo;
import com.atlassian.plugins.slack.util.ErrorResponse;
import io.atlassian.fugue.Either;

import java.util.Optional;

/**
 * This class manages the dedicated channel of issues
 */
public interface DedicatedChannelManager {
    /**
     * Returns a notification info if there is a dedicated channel that needs to be notified
     *
     * @param event the event
     * @return an Option
     */
    Optional<NotificationInfo> getNotificationsFor(JiraIssueEvent event);

    /**
     * Assigns a new dedicated channel in Slack for the given issue
     *
     * @return the channel you've always wanted
     */
    Either<ErrorResponse, DedicatedChannel> assignDedicatedChannel(Issue issue, String teamId, String channelId);

    /**
     * Un-assigns a dedicated channel in Slack for the given issue
     *
     * @param issue because, you have issues
     * @return void
     */
    Optional<ErrorResponse> unassignDedicatedChannel(Issue issue);

    /**
     * Returns the dedicated channel for the given issue if any
     *
     * @param issue because, you have issues
     * @return the channel you've always wanted
     */
    Optional<DedicatedChannel> getDedicatedChannel(Issue issue);

    /**
     * Checks if the current user has permission to assign a dedicated channel to the issue
     *
     * @param issue issue to assign a dedicated channel to
     * @return true if the user can assign a dedicated channel to the issue, false otherwise
     */
    boolean canAssignDedicatedChannel(Issue issue);

    /**
     * Return true if the given channel id and the optional dedicated channel (if any) are different channels.
     *
     * @param channelId        the first channel's id
     * @param dedicatedChannel an option which may contain a second channel
     * @return
     */
    boolean isNotSameChannel(final String channelId, Optional<DedicatedChannel> dedicatedChannel);
}
