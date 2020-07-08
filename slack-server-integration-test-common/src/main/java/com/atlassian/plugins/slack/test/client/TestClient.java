package com.atlassian.plugins.slack.test.client;

import com.atlassian.plugins.slack.admin.InstallationCompletionData;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.client.DefaultSlackClient;
import com.atlassian.plugins.slack.api.webhooks.LinkSharedSlackEvent;
import com.atlassian.plugins.slack.rest.model.GetWorkspacesResponse;
import com.atlassian.plugins.slack.rest.model.OauthRequestData;
import com.atlassian.plugins.slack.test.EventResponse;
import com.atlassian.plugins.slack.test.TestChannels;
import com.atlassian.plugins.slack.test.TestTeams;
import com.atlassian.plugins.slack.test.TestUsers;
import com.atlassian.plugins.slack.test.UserCredentials;
import com.atlassian.plugins.slack.test.mockserver.SlackMockServer;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import okhttp3.Call;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.Credentials;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.github.seratch.jslack.app_backend.SlackSignature.HeaderNames.X_SLACK_REQUEST_TIMESTAMP;
import static com.github.seratch.jslack.app_backend.SlackSignature.HeaderNames.X_SLACK_SIGNATURE;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;

public class TestClient {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final MediaType GENERIC = MediaType.parse("*/*; charset=utf-8");
    private static final String REST_PATH = "rest/slack/latest/";

    private final HttpUrl baseUrl;
    protected final OkHttpClient client;
    private final Gson gson = new Gson();
    private final SlackLinkClient slackLinkClient = new SlackLinkClient();
    private final SlackOAuhClient slackOAuhClient = new SlackOAuhClient();
    private final EventsClient events = new EventsClient();
    private final Map<String, List<Cookie>> cookieMap = new HashMap<>();
    private String tag;

    public TestClient(final String baseUrl, final UserCredentials userCredentials) {
        this.baseUrl = HttpUrl.get(baseUrl).newBuilder().build();
        this.tag = "";
        this.client = new OkHttpClient.Builder()
                .cookieJar(new CookieJar() {
                    @Override
                    public void saveFromResponse(final HttpUrl url, final List<Cookie> cookies) {
                        synchronized (cookieMap) {
                            cookieMap.computeIfAbsent(url.host(), v -> new ArrayList<>()).addAll(cookies);
                        }
                    }

                    @Override
                    public List<Cookie> loadForRequest(final HttpUrl url) {
                        return cookieMap.computeIfAbsent(url.host(), v -> new ArrayList<>());
                    }
                })
                .addInterceptor(chain -> chain.proceed(chain
                        .request()
                        .newBuilder()
                        .header("Authorization", Credentials.basic(
                                userCredentials.getUsername(),
                                userCredentials.getPassword()))
                        .build()))
                .build();
    }

    /**
     * A magical value that will be appended to all tokens allowing us to identify requests relates to the tag.
     */
    public void setTestTag(final String tag) {
        this.tag = tag;
    }

    public Optional<HttpUrl> getRedirectBaseUrl() {
        try (Response response = execute(client.newBuilder()
                .followRedirects(false)
                .followSslRedirects(false)
                .build()
                .newCall(new Request.Builder().url(baseUrl).get().build()))) {
            final String location = response.header("location");
            return Optional.ofNullable(location)
                    .map(l -> l.replace("/dashboard", "").replaceAll("/$", ""))
                    .map(HttpUrl::get);
        } catch (Throwable e) {
            return Optional.empty();
        }
    }

    public SlackLinkClient slackLink() {
        return slackLinkClient;
    }

    public class SlackLinkClient {
        public GetWorkspacesResponse fetchSlackLinks() {
            try (Response response = get(withRestUrl("connection").build())) {
                return gson.fromJson(response.body().string(), GetWorkspacesResponse.class);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public void deleteSlackLink(String teamId) {
            delete(withRestUrl("connection").addPathSegment(teamId).build());
        }

        public void deleteDummyConnections() {
            fetchSlackLinks().getWorkspaces().forEach(team -> {
                if (team.getTeamId().startsWith(DefaultSlackClient.TEAM_DUMMY_PREFIX)) {
                    try {
                        deleteSlackLink(team.getTeamId());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }

        public InstallationCompletionData connectTeam(SlackLink link, boolean custom) {
            JsonObject json = new JsonObject();
            json.addProperty("client_id", link.getClientId());
            json.addProperty("client_secret", link.getClientSecret());
            json.addProperty("verification_token", link.getVerificationToken());
            json.addProperty("access_token", appendTag(link.getAccessToken()));
            json.addProperty("signing_secret", link.getSigningSecret());
            json.addProperty("bot_access_token", appendTag(link.getBotAccessToken()));
            json.addProperty("app_id", link.getAppId());
            json.addProperty("app_blueprint_id", link.getAppBlueprintId());
            json.addProperty("user_id", link.getUserId());
            json.addProperty("team_name", link.getTeamName());
            json.addProperty("team_id", link.getTeamId());
            json.addProperty("app_configuration_url", link.getAppConfigurationUrl());
            json.addProperty("bot_user_id", link.getBotUserId());
            json.addProperty("bot_username", link.getBotUserName());
            if (custom) {
                json.addProperty("custom", true);
            }

            return parseResponse(postJson(withRestUrl("connection").build(), json), InstallationCompletionData.class);
        }
    }

    public SlackOAuhClient oAuth() {
        return slackOAuhClient;
    }

    public class SlackOAuhClient {
        public void confirmAccount(String teamId, String slackUserId) {
            final HttpUrl originalUrl = HttpUrl.get(SlackMockServer.STATIC_BASE_URL + "?v=1#123");

            //begin request
            final HttpUrl redirectToSlackUrl;
            try (Response beginResponse = postJson(
                    withRestUrl("oauth/begin/" + teamId).build(),
                    new OauthRequestData(SlackMockServer.STATIC_BASE_URL, originalUrl.query(), originalUrl.fragment()))) {
                redirectToSlackUrl = HttpUrl.get(beginResponse.body().string());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            //complete request
            try (Response completeResponse = execute(client.newBuilder()
                    .followRedirects(false)
                    .followSslRedirects(false)
                    .build()
                    .newCall(
                            new Request.Builder().url(withRestUrl("oauth/redirect")
                                    .addPathSegment(teamId)
                                    .addQueryParameter("state", redirectToSlackUrl.queryParameter("state"))
                                    .addQueryParameter("code", "secret.code:" + slackUserId)
                                    .build()
                            )
                                    .get()
                                    .build()
                    ))) {
                assertThat(completeResponse.header("location"), endsWith("?v=1#123"));
            }
        }

        public boolean hasUserConfirmed(String teamId) {
            try (Response response = get(withRestUrl("connection-status")
                    .addPathSegment(teamId)
                    .addPathSegment("user")
                    .build())) {
                return response.code() == 200;
            }
        }

        public void unlinkAccount(String teamId) {
            delete(withRestUrl("oauth").addPathSegment(teamId).build());
        }
    }

    public EventsClient events() {
        return events;
    }

    public class EventsClient {
        public EventResponse linkShared(String... links) {
            JsonArray linksArray = new JsonArray();
            for (String link : links) {
                JsonObject linkObj = new JsonObject();
                linkObj.addProperty("url", link);
                linkObj.addProperty("domain", getDomain(link));
                linksArray.add(linkObj);
            }

            JsonObject event = new JsonObject();
            event.addProperty("type", LinkSharedSlackEvent.TYPE);
            event.addProperty("team", TestTeams.DUMMY_TEAM_ID);
            event.addProperty("channel", TestChannels.PUBLIC.getId());
            event.addProperty("user", TestUsers.ADMIN_USER.getSlackUserId());
            event.addProperty("message_ts", "some-message-timestamp");
            event.add("links", linksArray);

            JsonObject payload = new JsonObject();
            payload.addProperty("team_id", TestTeams.DUMMY_TEAM_ID);
            payload.add("event", event);

            Headers header = Headers.of(X_SLACK_REQUEST_TIMESTAMP, String.valueOf(Instant.now().getEpochSecond()),
                    X_SLACK_SIGNATURE, "dummy-signature");
            return parseResponse(postJson(withRestUrl("event").build(), payload, header), EventResponse.class);
        }

        private String getDomain(String link) {
            try {
                return new URI(link).getHost();
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public Response visitPage(String relativePath) {
        return get(withRelativeUrl(relativePath).build());
    }

    protected Response get(HttpUrl url) {
        return execute(new Request.Builder().url(url).get().build());
    }

    protected Response postJson(HttpUrl url, Object payload) {
        return postJson(url, payload, null);
    }

    protected Response postJson(HttpUrl url, Object payload, Headers headers) {
        final RequestBody body;
        if (payload instanceof String) {
            body = RequestBody.create(JSON, (String) payload);
        } else if (payload instanceof JsonElement) {
            body = RequestBody.create(JSON, gson.toJson((JsonElement) payload));
        } else {
            body = RequestBody.create(JSON, gson.toJson(payload));
        }
        Request.Builder builder = new Request.Builder().url(url).post(body);
        if (headers != null) {
            builder.headers(headers);
        }
        return execute(builder.build());
    }

    private Response post(HttpUrl url) {
        return execute(new Request.Builder().url(url).post(emptyBody()).build());
    }

    protected Response put(HttpUrl url) {
        return execute(new Request.Builder().url(url).put(emptyBody()).build());
    }

    private Response putJson(HttpUrl url, Object body) {
        return execute(new Request.Builder().url(url).put(RequestBody.create(JSON, gson.toJson(body))).build());
    }

    private Response patch(HttpUrl url) {
        return execute(new Request.Builder().url(url).patch(emptyBody()).build());
    }

    private Response patchJson(HttpUrl url, Object body) {
        return execute(new Request.Builder().url(url).patch(RequestBody.create(JSON, gson.toJson(body))).build());
    }

    protected Response delete(HttpUrl url) {
        return execute(new Request.Builder().url(url).delete().build());
    }

    protected Response deleteJson(HttpUrl url, Object body) {
        return execute(new Request.Builder().url(url).delete(RequestBody.create(JSON, gson.toJson(body))).build());
    }

    protected <T> T parseResponse(Response response, Class<T> clazz) {
        if (response.code() >= 200 && response.code() < 300) {
            try (ResponseBody body = response.body()) {
                return gson.fromJson(body.string(), clazz);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        throw new RestException(response);
    }

    private RequestBody emptyBody() {
        return RequestBody.create(GENERIC, new byte[]{});
    }

    private HttpUrl.Builder withRelativeUrl(HttpUrl relativeUrl) {
        return baseUrl.newBuilder()
                .addEncodedPathSegments(relativeUrl.encodedPath())
                .encodedQuery(relativeUrl.encodedQuery())
                .encodedFragment(relativeUrl.encodedFragment());
    }

    protected HttpUrl.Builder withRelativeUrl(String path) {
        return baseUrl.newBuilder()
                .addPathSegments(path);
    }

    private HttpUrl.Builder withRestUrl(HttpUrl relativeUrl) {
        return baseUrl.newBuilder()
                .addPathSegments(REST_PATH)
                .addEncodedPathSegments(relativeUrl.encodedPath())
                .encodedQuery(relativeUrl.encodedQuery())
                .encodedFragment(relativeUrl.encodedFragment());
    }

    protected HttpUrl.Builder withRestUrl(String path) {
        return baseUrl.newBuilder()
                .addPathSegments(REST_PATH)
                .addPathSegments(path);
    }

    private Response execute(Request req) {
        try {
            return client.newCall(req).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected Response execute(Call call) {
        try {
            return call.execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String appendTag(final String token) {
        return token + (isNotBlank(tag) ? "#" + tag : "");
    }
}
