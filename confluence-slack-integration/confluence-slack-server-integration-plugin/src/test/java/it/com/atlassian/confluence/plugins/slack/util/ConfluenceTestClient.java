package it.com.atlassian.confluence.plugins.slack.util;

import com.atlassian.plugins.slack.test.UserCredentials;
import com.atlassian.plugins.slack.test.client.TestClient;

public class ConfluenceTestClient extends TestClient {
    private final NotificationsClient notifications = new NotificationsClient();

    public ConfluenceTestClient(final String baseUrl, final UserCredentials userCredentials) {
        super(baseUrl, userCredentials);
    }

    public NotificationsClient notifications() {
        return notifications;
    }

    public class NotificationsClient {
        public void disableAll(String spaceKey, String teamId, String channelId) {
            delete(withRestUrl("config")
                    .addPathSegment(spaceKey)
                    .addPathSegment(teamId)
                    .addPathSegment(channelId)
                    .build()).close();
        }

        public void disable(String spaceKey, String teamId, String channelId, String notificationKey) {
            delete(withRestUrl("config")
                    .addPathSegment(spaceKey)
                    .addPathSegment(teamId)
                    .addPathSegment(channelId)
                    .addPathSegment(notificationKey)
                    .build()).close();
        }

        public void enable(String spaceKey, String teamId, String channelId, String notificationKey, boolean initialLink) {
            put(withRestUrl("config")
                    .addPathSegment(spaceKey)
                    .addPathSegment(teamId)
                    .addPathSegment(channelId)
                    .addPathSegment(notificationKey)
                    .addQueryParameter("initialLink", String.valueOf(initialLink))
                    .build()).close();
        }
    }
}
