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
import lombok.Value;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static com.google.common.base.Strings.isNullOrEmpty;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;

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

    @Autowired
    public ConfigurationRestService(final PluginConfigurationManager pluginConfigurationManager,
                                    final ProjectConfigurationManager projectConfigurationManager,
                                    @Qualifier("salUserManager") final UserManager userManager,
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
        if (!checkAccessAsAdmin()) {
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
                return Response.status(BAD_REQUEST).build();
            }

            if (!permissionManager.hasPermission(ProjectPermissions.ADMINISTER_PROJECTS, project, context.getLoggedInUser())) {
                return Response.status(FORBIDDEN).build();
            }

            projectConfigurationManager.setProjectAutoConvertEnabled(project, allowAutoConvert);
            projectConfigurationManager.setIssuePanelHidden(project, hideIssuePanel);
            projectConfigurationManager.setSendRestrictedCommentsToDedicatedChannels(project, sendRestrictedCommentsToDedicated);
        } else {
            if (!checkAccessAsAdmin()) {
                return Response.status(FORBIDDEN).build();
            }

            final boolean guestChannelsEnabled = Boolean.parseBoolean(data.guestChannelsEnabled);
            pluginConfigurationManager.setGlobalAutoConvertEnabled(allowAutoConvert);
            pluginConfigurationManager.setIssuePreviewForGuestChannelsEnabled(guestChannelsEnabled);
            pluginConfigurationManager.setIssuePanelHidden(hideIssuePanel);
        }

        return Response.ok().build();
    }

    private boolean checkAccessAsAdmin() {
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
