package com.atlassian.jira.plugins.slack.manager.impl;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.comments.Comment;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.plugins.slack.dao.DedicatedChannelDAO;
import com.atlassian.jira.plugins.slack.manager.DedicatedChannelManager;
import com.atlassian.jira.plugins.slack.manager.ProjectConfigurationManager;
import com.atlassian.jira.plugins.slack.model.DedicatedChannel;
import com.atlassian.jira.plugins.slack.model.EventMatcherType;
import com.atlassian.jira.plugins.slack.model.dto.DedicatedChannelDTO;
import com.atlassian.jira.plugins.slack.model.event.DedicatedChannelLinkedEvent;
import com.atlassian.jira.plugins.slack.model.event.DedicatedChannelUnlinkedEvent;
import com.atlassian.jira.plugins.slack.model.event.JiraIssueEvent;
import com.atlassian.jira.plugins.slack.service.notification.NotificationInfo;
import com.atlassian.jira.plugins.slack.util.CommentUtil;
import com.atlassian.jira.plugins.slack.util.PluginConstants;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugins.slack.analytics.AnalyticsContextProvider;
import com.atlassian.plugins.slack.api.client.SlackClient;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.atlassian.plugins.slack.api.notification.Verbosity;
import com.atlassian.plugins.slack.api.webhooks.ChannelDeletedSlackEvent;
import com.atlassian.plugins.slack.event.SlackTeamUnlinkedEvent;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.plugins.slack.util.AutoSubscribingEventListener;
import com.atlassian.plugins.slack.util.ErrorResponse;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.UrlMode;
import com.github.seratch.jslack.api.model.Conversation;
import io.atlassian.fugue.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.UriBuilder;
import java.util.Optional;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

@Component("dedicatedChannelManager")
public class DefaultDedicatedChannelManager extends AutoSubscribingEventListener implements DedicatedChannelManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultDedicatedChannelManager.class);

    private final DedicatedChannelDAO dedicatedChannelDAO;
    private final SlackLinkManager slackLinkManager;
    private final JiraAuthenticationContext jiraAuthenticationContext;
    private final ApplicationProperties applicationProperties;
    private final PermissionManager permissionManager;
    private final SlackClientProvider slackClientProvider;
    private final AnalyticsContextProvider analyticsContextProvider;
    private final ProjectConfigurationManager projectConfigurationManager;

    @Autowired
    public DefaultDedicatedChannelManager(final DedicatedChannelDAO dedicatedChannelDAO,
                                          final SlackLinkManager slackLinkManager,
                                          final JiraAuthenticationContext jiraAuthenticationContext,
                                          final EventPublisher eventPublisher,
                                          final @Qualifier("salApplicationProperties") ApplicationProperties applicationProperties,
                                          final PermissionManager permissionManager,
                                          final SlackClientProvider slackClientProvider,
                                          final AnalyticsContextProvider analyticsContextProvider,
                                          final ProjectConfigurationManager projectConfigurationManager) {
        super(eventPublisher);
        this.dedicatedChannelDAO = dedicatedChannelDAO;
        this.slackLinkManager = slackLinkManager;
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.applicationProperties = applicationProperties;
        this.permissionManager = permissionManager;
        this.slackClientProvider = slackClientProvider;
        this.analyticsContextProvider = analyticsContextProvider;
        this.projectConfigurationManager = projectConfigurationManager;
    }

    /**
     * Returns the notifications infos for this particular issue event
     *
     * @param event the issue event
     * @return an Option
     */
    @Override
    public Optional<NotificationInfo> getNotificationsFor(final JiraIssueEvent event) {
        final EventMatcherType eventMatcher = event.getEventMatcher();
        if (!PluginConstants.EVENT_MATCHERS_FOR_DEDICATED_CHANNEL.contains(eventMatcher)) {
            return Optional.empty();
        }

        // restricted comments notification should not be sent to dedicated channels
        final Comment comment = event.getComment().orElse(null);
        final Issue issue = event.getIssue();
        final boolean isRestrictedComment = eventMatcher == EventMatcherType.ISSUE_COMMENTED && CommentUtil.isRestricted(comment);
        if (isRestrictedComment && !projectConfigurationManager.shouldSendRestrictedCommentsToDedicatedChannels(issue.getProjectObject())) {
            return Optional.empty();
        }

        final Optional<DedicatedChannel> dedicatedChannel = dedicatedChannelDAO.getDedicatedChannel(issue.getId());

        return dedicatedChannel
                .flatMap(channel -> slackLinkManager.getLinkByTeamId(channel.getTeamId())
                        .map(link -> new NotificationInfo(
                                link, channel.getChannelId(), null, "", channel.getCreator(), Verbosity.EXTENDED))
                        .toOptional());
    }

    @EventListener
    public void onChannelDeletedEvent(final ChannelDeletedSlackEvent event) {
        dedicatedChannelDAO.findMappingsForChannel(event.getChannel()).forEach(
                dc -> dedicatedChannelDAO.deleteDedicatedChannel(dc.getIssueId()));
    }

    @EventListener
    public void onTeamDisconnection(final SlackTeamUnlinkedEvent event) {
        dedicatedChannelDAO.findMappingsByTeamId(event.getTeamId()).forEach(
                dc -> dedicatedChannelDAO.deleteDedicatedChannel(dc.getIssueId()));
    }

    @Override
    public Either<ErrorResponse, DedicatedChannel> assignDedicatedChannel(final Issue issue,
                                                                          final String teamId,
                                                                          final String channelId) {
        final Either<ErrorResponse, ApplicationUser> jiraUser = testJiraUserAccess(issue);
        if (jiraUser.isLeft()) {
            return Either.left(jiraUser.left().get());
        }

        ApplicationUser applicationUser = jiraUser.right().get();
        final Either<Throwable, SlackClient> slackClientOptional = slackClientProvider.withTeamId(teamId)
                .flatMap(client -> client
                        .withUserTokenIfAvailable(applicationUser.getKey())
                        .map(Either::<Throwable, SlackClient>right)
                        .orElseGet(() -> Either.left(new Exception("Account is not linked"))));
        if (slackClientOptional.isLeft()) {
            LOGGER.debug("Could not load slack link or user.", slackClientOptional.left().get());
            return Either.left(new ErrorResponse(new Exception(
                    "Could not load slack link or user. " + slackClientOptional.left().get().getMessage()),
                    BAD_REQUEST.getStatusCode()));
        }

        final SlackClient slackClient = slackClientOptional.getOrNull();
        try {
            final Either<ErrorResponse, Conversation> channelDetails = slackClient.getConversationsInfo(channelId);
            if (channelDetails.isLeft()) {
                return Either.left(new ErrorResponse(
                        new Exception("Couldn't find the specified channel."),
                        BAD_REQUEST.getStatusCode()));
            }

            final Conversation conversation = channelDetails.getOrNull();
            final DedicatedChannel newDedicatedChannel = new DedicatedChannelDTO(
                    issue.getId(),
                    conversation.getName(),
                    teamId,
                    conversation.getId(),
                    conversation.isPrivate(),
                    applicationUser.getKey());
            dedicatedChannelDAO.insertDedicatedChannel(newDedicatedChannel);

            final String topic = getTopic(issue);
            slackClient.setConversationTopic(conversation.getId(), topic);
            slackClient.selfInviteToConversation(conversation.getId());

            final DedicatedChannelLinkedEvent channelLinkEvent = new DedicatedChannelLinkedEvent(
                    analyticsContextProvider.byTeamIdAndUserKey(teamId, applicationUser.getKey()),
                    issue.getProjectId(), issue.getKey(), newDedicatedChannel.getChannelId(), newDedicatedChannel.getCreator());
            eventPublisher.publish(channelLinkEvent);

            return Either.right(newDedicatedChannel);
        } catch (Exception e) {
            return Either.left(new ErrorResponse(
                    new Exception("Encountered error while creating a dedicated channel."),
                    INTERNAL_SERVER_ERROR.getStatusCode()));
        }
    }

    @Override
    public Optional<ErrorResponse> unassignDedicatedChannel(final Issue issue) {
        Either<ErrorResponse, ApplicationUser> jiraUser = testJiraUserAccess(issue);
        if (jiraUser.isLeft()) {
            return Optional.of(jiraUser.left().get());
        }

        try {
            Optional<DedicatedChannel> dedicatedChannel = dedicatedChannelDAO.getDedicatedChannel(issue.getId());
            if (!dedicatedChannel.isPresent()) {
                LOGGER.info("This issue doesn't have a dedicated channel assigned to it: " + issue.getId());
                return Optional.empty();
            }
            DedicatedChannel channel = dedicatedChannel.get();

            dedicatedChannelDAO.deleteDedicatedChannel(issue.getId());
            eventPublisher.publish(new DedicatedChannelUnlinkedEvent(
                    analyticsContextProvider.byTeamIdAndUserKey(channel.getTeamId(), channel.getCreator()),
                    issue.getProjectId(), issue.getKey(), channel.getChannelId(), channel.getCreator()));

            return Optional.empty();
        } catch (Exception e) {
            return Optional.of(new ErrorResponse(
                    new Exception("Encountered error while creating a dedicated channel."),
                    INTERNAL_SERVER_ERROR.getStatusCode()));
        }
    }

    private Either<ErrorResponse, ApplicationUser> testJiraUserAccess(final Issue issue) {
        if (jiraAuthenticationContext.getLoggedInUser() == null) {
            return Either.left(new ErrorResponse(
                    new Exception("You are not logged in to Jira."),
                    FORBIDDEN.getStatusCode()));
        }
        if (!canAssignDedicatedChannel(issue)) {
            return Either.left(new ErrorResponse(
                    new Exception("You don't have enough permission to modify a dedicated channel from this issue."),
                    FORBIDDEN.getStatusCode()));
        } else {
            return Either.right(jiraAuthenticationContext.getLoggedInUser());
        }
    }

    private String getTopic(final Issue issue) {
        return String.format("%s - %s", getViewIssueUrl(issue), issue.getSummary());
    }

    private String getViewIssueUrl(final Issue issue) {
        return UriBuilder.fromPath("{baseUrl}/browse/{issueKey}")
                .build(applicationProperties.getBaseUrl(UrlMode.CANONICAL), issue.getKey())
                .toASCIIString();
    }

    @Override
    public Optional<DedicatedChannel> getDedicatedChannel(final Issue issue) {
        return dedicatedChannelDAO.getDedicatedChannel(issue.getId());
    }

    @Override
    public boolean canAssignDedicatedChannel(final Issue issue) {
        return permissionManager.hasPermission(ProjectPermissions.EDIT_ISSUES, issue, jiraAuthenticationContext.getLoggedInUser());
    }

    @Override
    public boolean isNotSameChannel(final String channelId, final Optional<DedicatedChannel> dedicatedChannel) {
        return dedicatedChannel.map(input -> !input.getChannelId().equals(channelId)).orElse(false);
    }
}
