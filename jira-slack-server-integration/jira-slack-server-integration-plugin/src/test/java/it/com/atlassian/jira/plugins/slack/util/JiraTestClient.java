package it.com.atlassian.jira.plugins.slack.util;

import com.atlassian.jira.plugins.slack.model.dto.ProjectConfigurationDTO;
import com.atlassian.plugins.slack.test.UserCredentials;
import com.atlassian.plugins.slack.test.client.TestClient;

public class JiraTestClient extends TestClient {
    private final NotificationsClient notifications = new NotificationsClient();

    public JiraTestClient(final String baseUrl, final UserCredentials userCredentials) {
        super(baseUrl, userCredentials);
    }

    public NotificationsClient notifications() {
        return notifications;
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
