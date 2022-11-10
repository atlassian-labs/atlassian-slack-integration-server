package it.com.atlassian.confluence.plugins.slack.util;

import com.atlassian.confluence.webdriver.pageobjects.ConfluenceTestedProduct;
import com.atlassian.plugins.slack.admin.InstallationCompletionData;
import com.atlassian.plugins.slack.test.ServerDiscovery;
import com.atlassian.plugins.slack.test.UserCredentials;
import com.atlassian.plugins.slack.test.client.TestClientExtension;
import com.atlassian.plugins.slack.test.mockserver.SlackMockServerExtension;
import okhttp3.Credentials;
import okhttp3.Interceptor.Chain;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static com.atlassian.confluence.test.api.model.person.UserWithDetails.ADMIN;
import static com.atlassian.plugins.slack.test.TestTeams.DUMMY_TEAM;
import static com.atlassian.plugins.slack.test.TestTeams.DUMMY_TEAM_ID;
import static com.atlassian.plugins.slack.test.TestUsers.ADMIN_USER;
import static com.atlassian.plugins.slack.test.TestUsers.REGULAR_USER;
import static com.github.seratch.jslack.api.methods.Methods.CHAT_POST_MESSAGE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public abstract class SlackFunctionalTestBase {
    final static Function<ExtensionContext, String> testNameProvider = context -> context.getRequiredTestMethod().getName();

    @RegisterExtension
    protected static TestClientExtension<ConfluenceTestClient> client = new TestClientExtension<>(
            ConfluenceTestClient::new,
            new UserCredentials(ADMIN.getUsername(), ADMIN.getPassword()),
            new UserCredentials(ADMIN.getUsername(), ADMIN.getPassword()),
            () -> ServerDiscovery.instance("confluence", ConfluenceTestedProduct.class,
                    new UserCredentials(ADMIN.getUsername(), ADMIN.getPassword()), Optional.of(1990)),
            testNameProvider);

    @RegisterExtension
    protected static SlackMockServerExtension server = new SlackMockServerExtension(
            () -> {
                String baseUrl = client.instance().getBaseUrl();
                return Collections.singleton(baseUrl);
            }, testNameProvider);

    public static final String SPACE_KEY = "IT";

    protected void connectToDummyTeamWithCustomInstall() {
        connectToDummyTeam(false);
    }

    protected void connectToDummyTeamWithCustomApp() {
        connectToDummyTeam(true);
    }

    protected void connectToDummyTeam(boolean custom) {
        server.clearHistoryExecuteAndWaitForNewRequest(CHAT_POST_MESSAGE, () -> {
            final InstallationCompletionData data = client.admin().slackLink().connectTeam(DUMMY_TEAM, custom);
            assertThat(data, is(new InstallationCompletionData(DUMMY_TEAM.getTeamName(), DUMMY_TEAM_ID)));
        });
    }

    protected void confirmAdminAccount() {
        client.admin().oAuth().confirmAccount(ADMIN_USER.getSlackTeamId(), ADMIN_USER.getSlackUserId());
    }

    protected void confirmUserAccount() {
        client.user().oAuth().confirmAccount(REGULAR_USER.getSlackTeamId(), REGULAR_USER.getSlackUserId());
    }

    protected void connectToDummyTeamAndConfirmAdminAccount() {
        connectToDummyTeamWithCustomInstall();
        confirmAdminAccount();
    }

    protected void connectToDummyTeamAndConfirmUserAccount() {
        connectToDummyTeamWithCustomInstall();
        confirmUserAccount();
    }

    protected void createTestSpace() {
        /*final AuthenticatedWebResourceProvider prov = AuthenticatedWebResourceProvider.createWithNewClient(client.instance().getBaseUrl());
        prov.setAuthContext(ADMIN.getUsername(), ADMIN.getPassword().toCharArray());
        final RemoteSpaceServiceImpl spaceService = new RemoteSpaceServiceImpl(prov, MoreExecutors.newDirectExecutorService());
        final Space testSpace = Space.builder().name("IT Space").key(SPACE_KEY).build();
        if (spaceService.find().withKeys(SPACE_KEY).fetchOne().claim().isEmpty()) {
            spaceService.create(testSpace, false).claim();
        }*/

        OkHttpClient httpClient = new OkHttpClient.Builder()
                .addInterceptor((Chain chain) -> {
                    String credential = Credentials.basic("admin", "admin");
                    Request authenticatedReq = chain.request().newBuilder().header("Authorization", credential)
                            .build();
                    return chain.proceed(authenticatedReq);
                }).build();
        Set<String> spaceKeys = getSpaceKeys(httpClient);
        if (!spaceKeys.contains(SPACE_KEY)) {
            createSpace(httpClient, SPACE_KEY, "IT Space");
        }
    }

    private Set<String> getSpaceKeys(OkHttpClient httpClient) {
        String url = client.instance().getBaseUrl() + "/rest/api/space";
        Request request = new Request.Builder().url(url).build();
        try (Response response = httpClient.newCall(request).execute()) {
            String responseStr = response.body().string();
            JSONObject responseObject = new JSONObject(responseStr);
            JSONArray resultsArray = responseObject.getJSONArray("results");
            Set<String> spaceKeys = new HashSet<>();
            for (int i = 0; i < resultsArray.length(); i++) {
                JSONObject spaceObject = resultsArray.getJSONObject(i);
                spaceKeys.add(spaceObject.getString("key"));
            }
            return spaceKeys;
        } catch (IOException | JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void createSpace(OkHttpClient httpClient, String spaceKey, String spaceName) {
        String bodyJson;
        try {
            JSONObject bodyObject = new JSONObject();
            bodyObject.put("key", spaceKey);
            bodyObject.put("name", spaceName);
            bodyJson = bodyObject.toString();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        RequestBody body = RequestBody.create(MediaType.get("application/json; charset=utf-8"),
                bodyJson.getBytes(StandardCharsets.UTF_8));
        String url = client.instance().getBaseUrl() + "/rest/api/space";
        Request request = new Request.Builder().url(url).post(body).build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("Unable to create a space. Response code: " + response.code()
                        + ". Body: " + response.body().string());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
