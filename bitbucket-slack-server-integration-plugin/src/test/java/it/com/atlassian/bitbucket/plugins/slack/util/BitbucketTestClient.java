package it.com.atlassian.bitbucket.plugins.slack.util;

import com.atlassian.plugins.slack.test.UserCredentials;
import com.atlassian.plugins.slack.test.client.TestClient;
import it.com.atlassian.bitbucket.plugins.slack.util.model.RepositoryResponse;
import okhttp3.HttpUrl;

import static com.atlassian.bitbucket.test.DefaultFuncTestData.getRepositoryPath;

public class BitbucketTestClient extends TestClient {
    private final NotificationsClient notifications = new NotificationsClient();
    private final BitbucketClient bitbucket = new BitbucketClient();

    public BitbucketTestClient(final String baseUrl, final UserCredentials userCredentials) {
        super(baseUrl, userCredentials);
    }

    public NotificationsClient notifications() {
        return notifications;
    }

    public class NotificationsClient {
        public void disableAll(int repositoryId, String teamId, String channelId) {
            delete(withRestUrl("config")
                    .addPathSegment(String.valueOf(repositoryId))
                    .addPathSegment(teamId)
                    .addPathSegment(channelId)
                    .build()).close();
        }

        public void disable(int repositoryId, String teamId, String channelId, String notificationKey) {
            delete(withRestUrl("config")
                    .addPathSegment(String.valueOf(repositoryId))
                    .addPathSegment(teamId)
                    .addPathSegment(channelId)
                    .addPathSegment(notificationKey)
                    .build()).close();
        }

        public void enable(int repositoryId, String teamId, String channelId, String notificationKey) {
            put(withRestUrl("config")
                    .addPathSegment(String.valueOf(repositoryId))
                    .addPathSegment(teamId)
                    .addPathSegment(channelId)
                    .addPathSegment(notificationKey)
                    .build()).close();
        }

        public void createNew(int repositoryId, String teamId, String channelId) {
            put(withRestUrl("config")
                    .addPathSegment(String.valueOf(repositoryId))
                    .addPathSegment(teamId)
                    .addPathSegment(channelId)
                    .addPathSegment("new")
                    .addQueryParameter("initialLink", "true")
                    .build()).close();
        }
    }

    public BitbucketClient bitbucket() {
        return bitbucket;
    }

    public class BitbucketClient {
        public RepositoryResponse getRepository(String projectKey, String repoSlug) {
            return parseResponse(
                    get(withRelativeUrl("rest/api/1.0" + getRepositoryPath(projectKey, repoSlug)).build()),
                    RepositoryResponse.class);

        }
    }

    public HttpUrl getDefaultBaseUrl() {
        return HttpUrl.get("http://localhost:7990/bitbucket");
    }
}
