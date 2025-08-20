package com.atlassian.jira.plugins.slack.web.rest;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.plugins.slack.manager.DedicatedChannelManager;
import com.atlassian.jira.plugins.slack.model.DedicatedChannelInfo;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@Path("/dedicatedchannel")
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
public class DedicatedChannelResource {
    private static final Logger log = LoggerFactory.getLogger(DedicatedChannelResource.class);

    private final DedicatedChannelManager dedicatedChannelManager;
    private final IssueManager issueManager;
    private final PermissionManager permissionManager;
    private final JiraAuthenticationContext jiraAuthenticationContext;

    @Inject
    public DedicatedChannelResource(final DedicatedChannelManager dedicatedChannelManager,
                                    final IssueManager issueManager,
                                    final PermissionManager permissionManager,
                                    final JiraAuthenticationContext jiraAuthenticationContext) {
        this.dedicatedChannelManager = dedicatedChannelManager;
        this.issueManager = issueManager;
        this.permissionManager = permissionManager;
        this.jiraAuthenticationContext = jiraAuthenticationContext;
    }

    @POST
    public Response assignDedicatedChannel(final DedicatedChannelInfo dedicatedChannelInfo) {
        final Optional<Issue> issue = findIssue(dedicatedChannelInfo.getIssueKey());
        if (!issue.isPresent()) {
            return Response.serverError().entity("Issue not found.").build();
        }
        log.debug("Assigning a dedicated channel: {}", dedicatedChannelInfo);
        return dedicatedChannelManager
                .assignDedicatedChannel(
                        issue.get(),
                        dedicatedChannelInfo.getTeamId(),
                        dedicatedChannelInfo.getChannelId())
                .fold(
                        e -> Response.status(e.getStatusCode()).entity(e).build(),
                        channel -> Response.ok(channel).build());
    }

    @GET
    @Path("{issueKey}")
    public Response getDedicatedChannel(@PathParam("issueKey") final String issueKey) {
        final Optional<Issue> issue = findIssue(issueKey);
        if (!issue.isPresent() || !permissionManager.hasPermission(
                ProjectPermissions.BROWSE_PROJECTS,
                issue.get(),
                jiraAuthenticationContext.getLoggedInUser())) {
            return Response.serverError().entity("Issue not found.").build();
        }
        return dedicatedChannelManager.getDedicatedChannel(issue.get())
                .map(channel -> Response.ok(channel).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    @DELETE
    public Response unassignDedicatedChannel(final DedicatedChannelInfo dedicatedChannelInfo) {
        final Optional<Issue> issue = findIssue(dedicatedChannelInfo.getIssueKey());
        if (!issue.isPresent()) {
            return Response.serverError().entity("Issue not found.").build();
        }
        log.debug("Unassigning a dedicated channel from {}", dedicatedChannelInfo);
        return dedicatedChannelManager.unassignDedicatedChannel(issue.get())
                .map(error -> Response.serverError().entity(error.getMessage()).build())
                .orElseGet(() -> Response.ok().build());
    }

    private Optional<Issue> findIssue(final String issueKey) {
        return Optional.ofNullable(issueKey)
                .flatMap(key -> Optional.ofNullable(issueManager.getIssueObject(key)));
    }
}
