package com.atlassian.jira.plugins.slack.web.rest;

import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.plugins.slack.manager.PluginConfigurationManager;
import com.atlassian.jira.plugins.slack.manager.ProjectConfigurationManager;
import com.atlassian.jira.plugins.slack.settings.JiraSettingsService;
import com.atlassian.jira.plugins.slack.web.rest.ConfigurationRestService.BulkEditNotificationsMode;
import com.atlassian.jira.plugins.slack.web.rest.ConfigurationRestService.SaveConfigurationData;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.plugins.slack.rest.model.LimitedSlackLinkDto;
import com.atlassian.sal.api.user.UserKey;
import com.atlassian.sal.api.user.UserManager;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import jakarta.ws.rs.core.Response;
import java.util.Optional;

import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static jakarta.ws.rs.core.Response.Status.NO_CONTENT;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ConfigurationRestServiceTest {
    private static final String USER = "USR";
    private static final UserKey userKey = new UserKey(USER);

    @Mock
    private PluginConfigurationManager pluginConfigurationManager;
    @Mock
    private ProjectConfigurationManager projectConfigurationManager;
    @Mock
    private UserManager userManager;
    @Mock
    private ProjectManager projectManager;
    @Mock
    private PermissionManager permissionManager;
    @Mock
    private JiraAuthenticationContext context;
    @Mock
    private SlackLinkManager slackLinkManager;
    @Mock
    private JiraSettingsService jiraSettingsService;

    @Mock
    private ApplicationUser applicationUser;
    @Mock
    private Project project;
    @Mock
    private SlackLink link;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private ConfigurationRestService target;

    @Test
    public void getConfiguration_shouldForbidIfNotAdmin() {
        when(userManager.getRemoteUserKey()).thenReturn(userKey);
        when(userManager.isAdmin(userKey)).thenReturn(false);
        when(userManager.isSystemAdmin(userKey)).thenReturn(false);

        Response result = target.getConfiguration();

        assertThat(result.getStatus(), is(FORBIDDEN.getStatusCode()));
    }

    @Test
    public void getConfiguration_shouldReturnNoContentIfNoLinkIsDisconnected() {
        when(userManager.getRemoteUserKey()).thenReturn(userKey);
        when(userManager.isAdmin(userKey)).thenReturn(true);
        when(slackLinkManager.isAnyLinkDisconnected()).thenReturn(Optional.empty());

        Response result = target.getConfiguration();

        assertThat(result.getStatus(), is(NO_CONTENT.getStatusCode()));
    }

    @Test
    public void getConfiguration_shouldReturnValueForAdmin() {
        when(userManager.getRemoteUserKey()).thenReturn(userKey);
        when(userManager.isAdmin(userKey)).thenReturn(true);
        when(slackLinkManager.isAnyLinkDisconnected()).thenReturn(Optional.of(link));
        when(link.getTeamId()).thenReturn("T");
        when(link.getAppConfigurationUrl()).thenReturn("U");
        when(link.getBotUserId()).thenReturn("BUI");
        when(link.getBotUserName()).thenReturn("BUN");
        when(link.getTeamName()).thenReturn("TN");

        Response result = target.getConfiguration();

        assertThat(result.getStatus(), is(OK.getStatusCode()));
        assertThat(((LimitedSlackLinkDto) result.getEntity()).getTeamId(), is("T"));
        assertThat(((LimitedSlackLinkDto) result.getEntity()).getAppConfigurationUrl(), is("U"));
        assertThat(((LimitedSlackLinkDto) result.getEntity()).getBotUserId(), is("BUI"));
        assertThat(((LimitedSlackLinkDto) result.getEntity()).getBotUserName(), is("BUN"));
        assertThat(((LimitedSlackLinkDto) result.getEntity()).getTeamName(), is("TN"));
    }

    @Test
    public void saveConfiguration_shouldEnableProjectOptions() {
        when(projectManager.getProjectByCurrentKey("P")).thenReturn(project);
        when(context.getLoggedInUser()).thenReturn(applicationUser);
        when(permissionManager.hasPermission(ProjectPermissions.ADMINISTER_PROJECTS, project, applicationUser)).thenReturn(true);

        SaveConfigurationData input = new SaveConfigurationData("P", "true", "true", "true", "true");
        Response result = target.saveConfiguration(input);

        assertThat(result.getStatus(), is(OK.getStatusCode()));
        verify(projectConfigurationManager).setProjectAutoConvertEnabled(project, true);
        verify(projectConfigurationManager).setIssuePanelHidden(project, true);
        verify(projectConfigurationManager).setSendRestrictedCommentsToDedicatedChannels(project, true);
    }

    @Test
    public void saveConfiguration_shouldForbidWhenNotProjectAdmin() {
        when(projectManager.getProjectByCurrentKey("P")).thenReturn(project);
        when(context.getLoggedInUser()).thenReturn(applicationUser);
        when(permissionManager.hasPermission(ProjectPermissions.ADMINISTER_PROJECTS, project, applicationUser)).thenReturn(false);

        SaveConfigurationData input = new SaveConfigurationData("P", "true", "true", "true", "true");
        Response result = target.saveConfiguration(input);

        assertThat(result.getStatus(), is(FORBIDDEN.getStatusCode()));
        verify(projectConfigurationManager, never()).setProjectAutoConvertEnabled(any(), anyBoolean());
    }

    @Test
    public void saveConfiguration_shouldEnableGlobalSettings() {
        when(userManager.getRemoteUserKey()).thenReturn(userKey);
        when(userManager.isAdmin(userKey)).thenReturn(true);

        SaveConfigurationData input = new SaveConfigurationData("", "true", "true", "true", "true");
        Response result = target.saveConfiguration(input);

        assertThat(result.getStatus(), is(OK.getStatusCode()));
        verify(pluginConfigurationManager).setGlobalAutoConvertEnabled(true);
        verify(pluginConfigurationManager).setIssuePreviewForGuestChannelsEnabled(true);
        verify(pluginConfigurationManager).setIssuePanelHidden(true);
    }

    @Test
    public void saveConfiguration_shouldForbidGlobalSettingsForNonAdmin() {
        when(userManager.getRemoteUserKey()).thenReturn(userKey);
        when(userManager.isAdmin(userKey)).thenReturn(false);
        when(userManager.isSystemAdmin(userKey)).thenReturn(false);

        SaveConfigurationData input = new SaveConfigurationData("", "true", "true", "true", "true");
        Response result = target.saveConfiguration(input);

        assertThat(result.getStatus(), is(FORBIDDEN.getStatusCode()));
        verify(pluginConfigurationManager, never()).setGlobalAutoConvertEnabled(anyBoolean());
        verify(pluginConfigurationManager, never()).setIssuePreviewForGuestChannelsEnabled(anyBoolean());
        verify(pluginConfigurationManager, never()).setIssuePanelHidden(anyBoolean());
    }

    @Test
    public void getBulkEditNotifications_shouldReturnExpectedFlagValue() {
        when(userManager.getRemoteUserKey()).thenReturn(userKey);
        when(jiraSettingsService.areBulkNotificationsMutedForUser(userKey)).thenReturn(true);

        Response result = target.getBulkEditNotifications();

        assertThat(result.getStatus(), is(OK.getStatusCode()));
        assertThat(result.getEntity(), is(new BulkEditNotificationsMode(true)));
    }

    @Test
    public void muteBulkEditNotifications_shouldSaveFlagValue() {
        when(userManager.getRemoteUserKey()).thenReturn(userKey);

        target.muteBulkEditNotifications();

        verify(jiraSettingsService).muteBulkOperationNotificationsForUser(userKey);
    }

    @Test
    public void unmuteBulkEditNotifications_shouldSaveFlagValue() {
        when(userManager.getRemoteUserKey()).thenReturn(userKey);

        target.unmuteBulkEditNotifications();

        verify(jiraSettingsService).unmuteBulkOperationNotificationsForUser(userKey);
    }
}
