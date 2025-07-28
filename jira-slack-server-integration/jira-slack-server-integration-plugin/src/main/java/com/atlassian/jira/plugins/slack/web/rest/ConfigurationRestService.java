package com.atlassian.jira.plugins.slack.web.rest;

import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.plugins.slack.manager.PluginConfigurationManager;
import com.atlassian.jira.plugins.slack.manager.ProjectConfigurationManager;
import com.atlassian.jira.plugins.slack.settings.JiraSettingsService;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.plugins.slack.rest.model.LimitedSlackLinkDto;
import com.atlassian.sal.api.user.UserKey;
import com.atlassian.sal.api.user.UserManager;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

import javax.inject.Inject;
import javax.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import static com.google.common.base.Strings.isNullOrEmpty;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;

/**
 * Rest Endpoint that will let us validate the configuration of the plugin
 */
@Path("/configuration")
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
public class ConfigurationRestService {
    private final PluginConfigurationManager pluginConfigurationManager;
    private final ProjectConfigurationManager projectConfigurationManager;
    private final UserManager userManager;
    private final ProjectManager projectManager;
    private final PermissionManager permissionManager;
    private final JiraAuthenticationContext context;
    private final SlackLinkManager slackLinkManager;
    private final JiraSettingsService jiraSettingsService;

    @Inject
    public ConfigurationRestService(final PluginConfigurationManager pluginConfigurationManager,
                                    final ProjectConfigurationManager projectConfigurationManager,
                                    @Named("salUserManager") final UserManager userManager,
                                    final ProjectManager projectManager,
                                    final PermissionManager permissionManager,
                                    final JiraAuthenticationContext context,
                                    final SlackLinkManager slackLinkManager,
                                    final JiraSettingsService jiraSettingsService) {
        this.pluginConfigurationManager = pluginConfigurationManager;
        this.projectConfigurationManager = projectConfigurationManager;
        this.userManager = userManager;
        this.projectManager = projectManager;
        this.permissionManager = permissionManager;
        this.context = context;
        this.slackLinkManager = slackLinkManager;
        this.jiraSettingsService = jiraSettingsService;
    }

    @GET
    @Path("/status")
    public Response getConfiguration() {
        if (!isCurrentUserAdmin()) {
            return Response.status(FORBIDDEN).build();
        }
        return slackLinkManager.isAnyLinkDisconnected()
                .map(link -> Response.ok(new LimitedSlackLinkDto(link)))
                .orElseGet(Response::noContent)
                .build();
    }

    @POST
    public Response saveConfiguration(final SaveConfigurationData data) {
        final boolean allowAutoConvert = Boolean.parseBoolean(data.allowAutoConvert);
        final boolean hideIssuePanel = Boolean.parseBoolean(data.hideIssuePanel);
        final boolean sendRestrictedCommentsToDedicated = Boolean.parseBoolean(data.sendRestrictedCommentsToDedicated);

        if (!isNullOrEmpty(data.projectKey)) {
            final Project project = projectManager.getProjectByCurrentKey(data.projectKey);

            if (project == null) {
                return Response.status(BAD_REQUEST).entity("Project not found").build();
            }

            if (!permissionManager.hasPermission(ProjectPermissions.ADMINISTER_PROJECTS, project, context.getLoggedInUser()) &&
                    !isCurrentUserAdmin()) {
                return Response.status(FORBIDDEN).entity("No permissions").build();
            }

            projectConfigurationManager.setProjectAutoConvertEnabled(project, allowAutoConvert);
            projectConfigurationManager.setIssuePanelHidden(project, hideIssuePanel);
            projectConfigurationManager.setSendRestrictedCommentsToDedicatedChannels(project, sendRestrictedCommentsToDedicated);
        } else {
            if (!isCurrentUserAdmin()) {
                return Response.status(FORBIDDEN).entity("No permissions").build();
            }

            final boolean guestChannelsEnabled = Boolean.parseBoolean(data.guestChannelsEnabled);
            pluginConfigurationManager.setGlobalAutoConvertEnabled(allowAutoConvert);
            pluginConfigurationManager.setIssuePreviewForGuestChannelsEnabled(guestChannelsEnabled);
            pluginConfigurationManager.setIssuePanelHidden(hideIssuePanel);
        }

        return Response.ok().build();
    }

    private boolean isCurrentUserAdmin() {
        final UserKey userKey = userManager.getRemoteUserKey();
        return userKey != null && (userManager.isAdmin(userKey) || userManager.isSystemAdmin(userKey));
    }

    @GET
    @Path("/bulk-edit-notifications")
    public Response getBulkEditNotifications() {
        boolean muted = jiraSettingsService.areBulkNotificationsMutedForUser(userManager.getRemoteUserKey());
        return Response.ok(new BulkEditNotificationsMode(muted)).build();
    }

    @POST
    @Path("/bulk-edit-notifications/mute")
    public Response muteBulkEditNotifications() {
        jiraSettingsService.muteBulkOperationNotificationsForUser(userManager.getRemoteUserKey());
        return Response.ok().build();
    }

    @POST
    @Path("/bulk-edit-notifications/unmute")
    public Response unmuteBulkEditNotifications() {
        jiraSettingsService.unmuteBulkOperationNotificationsForUser(userManager.getRemoteUserKey());
        return Response.ok().build();
    }

    public static class SaveConfigurationData {
        @JsonProperty
        private final String projectKey;
        @JsonProperty
        private final String allowAutoConvert;
        @JsonProperty
        private final String guestChannelsEnabled;
        @JsonProperty
        private final String hideIssuePanel;
        @JsonProperty
        private final String sendRestrictedCommentsToDedicated;

        @JsonCreator
        public SaveConfigurationData(@JsonProperty("projectKey") final String projectKey,
                                     @JsonProperty("allowAutoConvert") final String allowAutoConvert,
                                     @JsonProperty("guestChannelEnabled") final String guestChannelsEnabled,
                                     @JsonProperty("hideIssuePanel") final String hideIssuePanel,
                                     @JsonProperty("sendRestrictedCommentsToDedicated") final String sendRestrictedCommentsToDedicated) {
            this.projectKey = projectKey;
            this.allowAutoConvert = allowAutoConvert;
            this.guestChannelsEnabled = guestChannelsEnabled;
            this.hideIssuePanel = hideIssuePanel;
            this.sendRestrictedCommentsToDedicated = sendRestrictedCommentsToDedicated;
        }
    }

    @Value
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY)
    public static class BulkEditNotificationsMode {
        boolean muted;
    }
}
