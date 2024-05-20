package com.atlassian.jira.plugins.slack.web.rest;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.plugins.slack.model.analytics.IssueMentionDeletedEvent;
import com.atlassian.jira.plugins.slack.model.analytics.IssuePanelVisitedEvent;
import com.atlassian.jira.plugins.slack.model.mentions.IssueMentionViewResponseFactory;
import com.atlassian.jira.plugins.slack.service.mentions.IssueMentionService;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.plugins.slack.analytics.AnalyticsContextProvider;
import com.atlassian.plugins.slack.api.ConversationKey;
import com.atlassian.sal.api.user.UserManager;
import com.google.common.collect.ImmutableMap;
import io.atlassian.fugue.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;

@Path("issue-mentions")
@Produces({MediaType.APPLICATION_JSON})
public class SlackIssueMentionsResource {
    private static final Logger log = LoggerFactory.getLogger(SlackIssueMentionsResource.class);

    private final UserManager userManager;
    private final IssueMentionService issueMentionService;
    private final IssueManager issueManager;
    private final IssueMentionViewResponseFactory responseFactory;
    private final PermissionManager permissionManager;
    private final JiraAuthenticationContext authenticationContext;
    private final EventPublisher eventPublisher;
    private final AnalyticsContextProvider analyticsContextProvider;

    @Inject
    public SlackIssueMentionsResource(final UserManager userManager,
                                      final IssueMentionService issueMentionService,
                                      final IssueManager issueManager,
                                      final IssueMentionViewResponseFactory responseFactory,
                                      final PermissionManager permissionManager,
                                      final JiraAuthenticationContext authenticationContext,
                                      final EventPublisher eventPublisher,
                                      final AnalyticsContextProvider analyticsContextProvider) {
        this.userManager = userManager;
        this.permissionManager = permissionManager;
        this.authenticationContext = authenticationContext;
        this.issueManager = checkNotNull(issueManager);
        this.issueMentionService = checkNotNull(issueMentionService);
        this.responseFactory = checkNotNull(responseFactory);
        this.eventPublisher = eventPublisher;
        this.analyticsContextProvider = analyticsContextProvider;
    }

    @GET
    @Path("{issueKey}")
    public Response issueMentions(@PathParam("issueKey") final String issueKey) {
        return getIssueByKey(issueKey).fold(
                errorResponse(issueKey),
                issue -> issueMentionService.getIssueMentions(issue.getId())
                        .map(mentions -> responseFactory.createResponse(
                                mentions, userManager.getRemoteUser().getUserKey().getStringValue()))
                        .fold(errorResponse(issue.getKey()), load -> Response.ok(load).build())
        );
    }

    @GET
    @Path("{issueKey}/channels")
    public Response issueMentionChannels(@PathParam("issueKey") final String issueKey) {
        return getIssueByKey(issueKey).fold(
                errorResponse(issueKey),
                issue -> issueMentionService.getIssueMentions(issue.getId())
                        .map(mentions -> responseFactory.createResponse(
                                mentions, userManager.getRemoteUser().getUserKey().getStringValue()))
                        .map(mentionViews -> ImmutableMap.<String, Long>builder()
                                .put("channelCount", mentionViews.stream()
                                        .map(IssueMentionViewResponseFactory.IssueMentionViewItem::getChannel)
                                        .distinct()
                                        .count())
                                .put("mentionCount", (long) mentionViews.size())
                                .build()
                        )
                        .fold(errorResponse(issue.getKey()), load -> {
                            eventPublisher.publish(new IssuePanelVisitedEvent(analyticsContextProvider.current(),
                                    load.get("mentionCount")));
                            return Response.ok(load).build();
                        })
        );
    }

    @DELETE
    @Path("{teamId}/{channelId}/{messageTimestamp}")
    public Response deleteIssueMention(@PathParam("teamId") final String teamId,
                                       @PathParam("channelId") final String channelId,
                                       @PathParam("messageTimestamp") final String messageTimestamp) {
        issueMentionService.deleteMessageMention(new ConversationKey(teamId, channelId), messageTimestamp);
        eventPublisher.publish(new IssueMentionDeletedEvent(analyticsContextProvider.current()));

        return Response.ok().build();
    }

    private Function<Throwable, Response> errorResponse(final String issueKey) {
        return error -> {
            log.error("Failed to get issue mention details for issueKey=" + issueKey, error);
            return Response.serverError().entity("Failed to fetch issue.").build();
        };
    }

    private Either<Throwable, Issue> getIssueByKey(String issueKey) {
        try {
            final Issue issue = issueManager.getIssueByKeyIgnoreCase(issueKey);
            if (issue == null) {
                log.warn("Issue with key {} not found.", issueKey);
                return Either.left(new IllegalArgumentException("Issue not found."));
            }

            if (!permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, issue, authenticationContext.getLoggedInUser())) {
                final String message = "User " + authenticationContext.getLoggedInUser().getName() + " does not have permission to access issue " + issueKey;
                log.warn(message);
                return Either.left(new IllegalArgumentException(message));
            }

            return Either.right(issue);
        } catch (RuntimeException ex) {
            log.warn("Failed to fetch issue with key=" + issueKey, ex);
            return Either.left(ex);
        }
    }
}
