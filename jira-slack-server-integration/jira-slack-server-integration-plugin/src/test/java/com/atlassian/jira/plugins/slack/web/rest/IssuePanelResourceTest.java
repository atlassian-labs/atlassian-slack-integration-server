package com.atlassian.jira.plugins.slack.web.rest;

import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.user.UserKey;
import com.atlassian.sal.api.user.UserManager;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.core.Response;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;

public class IssuePanelResourceTest {
    @Mock
    UserManager userManager;
    @Mock
    com.atlassian.jira.user.util.UserManager jiraUserManager;
    @Mock
    IssueManager issueManager;
    @Mock
    PermissionManager permissionManager;

    @Mock
    ApplicationUser applicationUser;
    @Mock
    MutableIssue mutableIssue;

    @InjectMocks
    IssuePanelResource target;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Test
    public void panelData_shouldForbidAccessForUnauthorizedUser() {
        String issueId = "some-issue-id";
        String userKeyStr = "some-user-key";
        UserKey stubUserKey = new UserKey(userKeyStr);
        when(userManager.getRemoteUserKey()).thenReturn(stubUserKey);
        when(jiraUserManager.getUserByKey(userKeyStr)).thenReturn(applicationUser);
        when(issueManager.getIssueByKeyIgnoreCase(issueId)).thenReturn(mutableIssue);
        when(permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, mutableIssue, applicationUser))
                .thenReturn(false);

        Response response = target.panelData(issueId);

        assertThat(response, notNullValue());
        assertThat(response.getStatus(), equalTo(Response.Status.FORBIDDEN.getStatusCode()));
    }
}
