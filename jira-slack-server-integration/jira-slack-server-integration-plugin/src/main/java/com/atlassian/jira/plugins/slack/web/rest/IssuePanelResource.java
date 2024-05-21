package com.atlassian.jira.plugins.slack.web.rest;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.plugins.slack.manager.DedicatedChannelManager;
import com.atlassian.jira.plugins.slack.manager.PluginConfigurationManager;
import com.atlassian.jira.plugins.slack.model.DedicatedChannel;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.SlackUser;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.plugins.slack.rest.model.LimitedSlackLinkDto;
import com.atlassian.plugins.slack.spi.SlackLinkAccessManager;
import com.atlassian.plugins.slack.user.SlackUserManager;
import com.atlassian.sal.api.user.UserManager;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Path("/issuepanel")
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
public class IssuePanelResource {
    private final PluginConfigurationManager pluginConfigurationManager;
    private final SlackLinkAccessManager slackLinkAccessManager;
    private final UserManager userManager;
    private final com.atlassian.jira.user.util.UserManager jiraUserManager;
    private final IssueManager issueManager;
    private final PermissionManager permissionManager;
    private final GlobalPermissionManager globalPermissionManager;
    private final DedicatedChannelManager dedicatedChannelManager;
    private final SlackUserManager slackUserManager;
    private final SlackLinkManager slackLinkManager;
    private final SlackClientProvider slackClientProvider;

    @Inject
    @Autowired
    public IssuePanelResource(final PluginConfigurationManager pluginConfigurationManager,
                              final SlackLinkAccessManager slackLinkAccessManager,
                              @Qualifier("salUserManager") final UserManager userManager,
                              @Qualifier("jiraUserManager") final com.atlassian.jira.user.util.UserManager jiraUserManager,
                              final IssueManager issueManager,
                              final PermissionManager permissionManager,
                              final GlobalPermissionManager globalPermissionManager,
                              final DedicatedChannelManager dedicatedChannelManager,
                              final SlackUserManager slackUserManager,
                              final SlackLinkManager slackLinkManager,
                              final SlackClientProvider slackClientProvider) {
        this.pluginConfigurationManager = pluginConfigurationManager;
        this.slackLinkAccessManager = slackLinkAccessManager;
        this.userManager = userManager;
        this.jiraUserManager = jiraUserManager;
        this.issueManager = issueManager;
        this.permissionManager = permissionManager;
        this.globalPermissionManager = globalPermissionManager;
        this.dedicatedChannelManager = dedicatedChannelManager;
        this.slackUserManager = slackUserManager;
        this.slackLinkManager = slackLinkManager;
        this.slackClientProvider = slackClientProvider;
    }

    @POST
    @Path("/hide")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response hidePanel(@Context HttpServletRequest httpRequest) {
        if (!slackLinkAccessManager.hasAccess(userManager.getRemoteUser(), httpRequest)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        pluginConfigurationManager.setIssuePanelHidden(true);
        return Response.ok().build();
    }

    @POST
    @Path("/show")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response showPanel() {
        pluginConfigurationManager.setIssuePanelHidden(false);
        return Response.ok().build();
    }

    @GET
    @Path("/data/{issueId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response panelData(@PathParam("issueId") String issueId) {
        if (userManager.getRemoteUser() == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        final ApplicationUser applicationUser = jiraUserManager.getUserByKey(
                userManager.getRemoteUser().getUserKey().getStringValue());
        final Issue issue = issueManager.getIssueByKeyIgnoreCase(issueId);

        if (issue == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        if (!permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, issue, applicationUser)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        final Project project = Objects.requireNonNull(issue.getProjectObject());

        final List<SlackLink> links = slackLinkManager.getLinks();

        // return linked slack users
        final List<LimitedSlackLinkDto> nonConfirmedLinks = new ArrayList<>();
        boolean hasConfirmedLinks = false;
        for (SlackLink link : links) {
            final boolean isLinked = slackClientProvider
                    .withLink(link)
                    .withUserTokenIfAvailable(applicationUser.getKey())
                    .isPresent();
            if (isLinked) {
                hasConfirmedLinks = true;
            } else {
                nonConfirmedLinks.add(new LimitedSlackLinkDto(link));
            }
        }

        boolean userCanAccessChannel = false;
        final Optional<DedicatedChannel> dedicatedChannelOptional = dedicatedChannelManager.getDedicatedChannel(issue);
        if (dedicatedChannelOptional.isPresent()) {
            final DedicatedChannel channel = dedicatedChannelOptional.get();
            final Optional<SlackUser> slackUser = slackUserManager
                    .getByTeamIdAndUserKey(channel.getTeamId(), applicationUser.getKey());
            if (slackUser.isPresent()) {
                userCanAccessChannel = slackClientProvider
                        .withTeamId(channel.getTeamId())
                        .toOptional()
                        .flatMap(client -> client.withUserTokenIfAvailable(slackUser.get()))
                        .map(client -> client.getConversationsInfo(channel.getChannelId()).isRight())
                        .orElse(false);
            }
        }

        return Response.ok(new IssuePanelData(
                permissionManager.hasPermission(ProjectPermissions.ADMINISTER_PROJECTS, project, applicationUser),
                globalPermissionManager.hasPermission(GlobalPermissionKey.ADMINISTER, applicationUser),
                hasConfirmedLinks,
                project.getKey(),
                issue.getKey(),
                dedicatedChannelOptional.orElse(null),
                dedicatedChannelManager.canAssignDedicatedChannel(issue),
                userCanAccessChannel,
                nonConfirmedLinks)
        ).build();
    }

    public static class IssuePanelData {
        @JsonProperty
        private final boolean isProjectAdmin;
        @JsonProperty
        private final boolean isAdmin;
        @JsonProperty
        private final boolean isLoggedIn;
        @JsonProperty
        private final String projectKey;
        @JsonProperty
        private final String issueKey;
        @JsonProperty
        private final DedicatedChannel dedicatedChannel;
        @JsonProperty
        private final boolean canAssignChannel;
        @JsonProperty
        private final boolean userCanAccessChannel;
        @JsonProperty
        private final List<LimitedSlackLinkDto> notConfirmedLinks;

        IssuePanelData(boolean isProjectAdmin,
                       boolean isAdmin,
                       boolean isLoggedIn,
                       String projectKey,
                       String issueKey,
                       DedicatedChannel dedicatedChannel,
                       boolean canAssignChannel,
                       boolean userCanAccessChannel,
                       List<LimitedSlackLinkDto> notConfirmedLinks) {
            this.isProjectAdmin = isProjectAdmin;
            this.isAdmin = isAdmin;
            this.isLoggedIn = isLoggedIn;
            this.projectKey = projectKey;
            this.issueKey = issueKey;
            this.dedicatedChannel = dedicatedChannel;
            this.canAssignChannel = canAssignChannel;
            this.userCanAccessChannel = userCanAccessChannel;
            this.notConfirmedLinks = notConfirmedLinks;
        }

        public boolean isLoggedIn() {
            return isLoggedIn;
        }

        public boolean isUserCanAccessChannel() {
            return userCanAccessChannel;
        }

        public boolean isProjectAdmin() {
            return isProjectAdmin;
        }

        public boolean isAdmin() {
            return isAdmin;
        }

        public String getProjectKey() {
            return projectKey;
        }

        public String getIssueKey() {
            return issueKey;
        }

        public DedicatedChannel getDedicatedChannel() {
            return dedicatedChannel;
        }

        public boolean isCanAssignChannel() {
            return canAssignChannel;
        }

        public List<LimitedSlackLinkDto> getNotConfirmedLinks() {
            return notConfirmedLinks;
        }
    }
}
