package it.com.atlassian.jira.plugins.slack.util;

import com.atlassian.jira.plugins.slack.model.dto.ProjectConfigurationDTO;
import com.atlassian.jira.plugins.slack.web.rest.IssuePanelResource.IssuePanelData;
import com.atlassian.plugins.slack.test.UserCredentials;
import com.atlassian.plugins.slack.test.client.TestClient;

public class JiraTestClient extends TestClient {
    private final NotificationsClient notifications = new NotificationsClient();
    private final IssuePanelClient issuePanel = new IssuePanelClient();

    public JiraTestClient(final String baseUrl, final UserCredentials userCredentials) {
        super(baseUrl, userCredentials);
    }

    public NotificationsClient notifications() {
        return notifications;
    }

    public IssuePanelClient issuePanel() {
        return issuePanel;
    }

    public class IssuePanelClient {
        // GET rest/slack/latest/issuepanel/data/{issueKey} - backs the issue-view Slack panel
        public IssuePanelData fetchData(String issueKey) {
            return parseResponse(
                    get(withRestUrl("issuepanel/data").addPathSegment(issueKey).build()),
                    IssuePanelData.class);
        }
    }

    public class NotificationsClient {
        private ProjectConfigurationDTO.Builder configBuilder(String groupId,
                                                              long projectId,
                                                              String projectKey,
                                                              String teamId,
                                                              String channelId) {
            return ProjectConfigurationDTO.builder()
                    .setProjectId(projectId)
                    .setProjectKey(projectKey)
                    .setTeamId(teamId)
                    .setChannelId(channelId)
                    .setConfigurationGroupId(groupId);
        }

        public void disableAll(String groupId, long projectId, String projectKey, String teamId, String channelId) {
            ProjectConfigurationDTO dto = configBuilder(groupId, projectId, projectKey, teamId, channelId).build();
            deleteJson(withRestUrl("mapping").addPathSegment(projectKey).build(), dto).close();
        }

        public void disable(String groupId, long projectId, String projectKey, String teamId, String channelId, String notificationKey) {
            ProjectConfigurationDTO dto = configBuilder(groupId, projectId, projectKey, teamId, channelId)
                    .setName(notificationKey)
                    .build();
            deleteJson(withRestUrl("mapping").addPathSegment(projectKey).build(), dto).close();
        }

        public void enable(String groupId, long projectId, String projectKey, String teamId, String channelId, String notificationKey) {
            ProjectConfigurationDTO dto = configBuilder(groupId, projectId, projectKey, teamId, channelId)
                    .setName(notificationKey)
                    .setValue("true")
                    .build();
            postJson(withRestUrl("mapping").addPathSegment(projectKey).build(), dto).close();
        }

        public void createNew(String groupId, long projectId, String projectKey, String teamId, String channelId) {
            ProjectConfigurationDTO dto = configBuilder(groupId, projectId, projectKey, teamId, channelId).build();
            postJson(withRestUrl("mapping").addPathSegment(projectKey).build(), dto).close();
        }
    }
}
