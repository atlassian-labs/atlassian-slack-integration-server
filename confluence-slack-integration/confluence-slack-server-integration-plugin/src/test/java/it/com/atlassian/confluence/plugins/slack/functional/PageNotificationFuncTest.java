package it.com.atlassian.confluence.plugins.slack.functional;

import com.atlassian.plugins.slack.test.RequestMatchers;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import it.com.atlassian.confluence.plugins.slack.util.SlackFunctionalTestBase;
import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.io.IOException;
import java.lang.reflect.Method;

import static com.atlassian.plugins.slack.test.RequestMatchers.hasHit;
import static com.atlassian.plugins.slack.test.TestChannels.PUBLIC;
import static com.atlassian.plugins.slack.test.TestTeams.DUMMY_TEAM;
import static com.github.seratch.jslack.api.methods.Methods.CHAT_POST_MESSAGE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

/**
 * Functional tests verifying that Confluence page and blog creation/update events
 * produce the correct {@code chat.postMessage} notifications to the Slack mock server.
 */
public class PageNotificationFuncTest extends SlackFunctionalTestBase {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASS = "admin";

    // Unique per test-run + per test-method to avoid Confluence "title already exists" 400s.
    // Confluence persists data across test runs so titles must never repeat.
    private static final String RUN_ID = String.valueOf(System.currentTimeMillis());
    private String PAGE_TITLE;
    private String BLOG_TITLE;

    private OkHttpClient httpClient;
    private String confluenceBaseUrl;

    @BeforeEach
    void setup(TestInfo testInfo) {
        // Unique title per run + per method — never conflicts with previous runs
        String suffix = RUN_ID + "-" + testInfo.getTestMethod().map(Method::getName).orElse("test");
        PAGE_TITLE = "Page Notification " + suffix;
        BLOG_TITLE = "Blog Notification " + suffix;

        confluenceBaseUrl = client.instance().getBaseUrl();
        httpClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request authenticatedReq = chain.request().newBuilder()
                            .header("Authorization", Credentials.basic(ADMIN_USER, ADMIN_PASS))
                            .build();
                    return chain.proceed(authenticatedReq);
                }).build();

        connectToDummyTeamWithCustomInstall();
        createTestSpace();
        enableNotification("PageCreate");
        enableNotification("PageUpdate");
        enableNotification("BlogCreate");
    }

    @Test
    void pageCreation_withNotificationEnabled_shouldPostNotification() {
        server.clearHistoryExecuteAndWaitForNewRequest(CHAT_POST_MESSAGE, () ->
                createPage(PAGE_TITLE));

        assertNotificationSent("created", "page", PAGE_TITLE);
    }

    @Test
    void pageCreation_notificationSentToCorrectChannel() {
        server.clearHistoryExecuteAndWaitForNewRequest(CHAT_POST_MESSAGE, () ->
                createPage(PAGE_TITLE));

        assertThat(server.requestHistoryForTest(), hasHit(CHAT_POST_MESSAGE, contains(allOf(
                RequestMatchers.requestEntityProperty(ChatPostMessageRequest::getChannel,
                        is(PUBLIC.getId()))
        ))));
    }

    @Test
    void pageCreation_withNotificationDisabled_shouldNotPostNotification() {
        disableNotification("PageCreate");

        server.clearHistoryExecuteAndExpectNoRequests(CHAT_POST_MESSAGE, () ->
                createPage(PAGE_TITLE));
    }

    @Test
    void pageUpdate_withNotificationEnabled_shouldPostNotification() {
        String pageId = createPageAndGetId(PAGE_TITLE + " for Update");

        server.clearHistoryExecuteAndWaitForNewRequest(CHAT_POST_MESSAGE, () ->
                updatePage(pageId, PAGE_TITLE + " for Update", 2));

        assertNotificationSent("updated", "page", PAGE_TITLE + " for Update");
    }

    @Test
    void pageUpdate_withNotificationDisabled_shouldNotPostNotification() {
        disableNotification("PageUpdate");
        String pageId = createPageAndGetId(PAGE_TITLE + " Silent Update");

        server.clearHistoryExecuteAndExpectNoRequests(CHAT_POST_MESSAGE, () ->
                updatePage(pageId, PAGE_TITLE + " Silent Update", 2));
    }

    @Test
    void blogCreation_withNotificationEnabled_shouldPostNotification() {
        server.clearHistoryExecuteAndWaitForNewRequest(CHAT_POST_MESSAGE, () ->
                createBlogPost(BLOG_TITLE));

        assertNotificationSent("created", "blog", BLOG_TITLE);
    }

    @Test
    void blogCreation_notificationSentToCorrectChannel() {
        server.clearHistoryExecuteAndWaitForNewRequest(CHAT_POST_MESSAGE, () ->
                createBlogPost(BLOG_TITLE));

        assertThat(server.requestHistoryForTest(), hasHit(CHAT_POST_MESSAGE, contains(allOf(
                RequestMatchers.requestEntityProperty(ChatPostMessageRequest::getChannel,
                        is(PUBLIC.getId()))
        ))));
    }

    @Test
    void blogCreation_withNotificationDisabled_shouldNotPostNotification() {
        disableNotification("BlogCreate");

        server.clearHistoryExecuteAndExpectNoRequests(CHAT_POST_MESSAGE, () ->
                createBlogPost(BLOG_TITLE));
    }

    /**
     * Asserts that exactly one {@code chat.postMessage} was sent to the public channel,
     * with notification text containing the action verb, content type, and content title.
     *
     * @param action      e.g. "created", "updated"
     * @param contentType e.g. "page", "blog"
     * @param title       the page or blog title
     */
    private void assertNotificationSent(String action, String contentType, String title) {
        assertThat(server.requestHistoryForTest(), hasHit(CHAT_POST_MESSAGE, contains(allOf(
                RequestMatchers.requestEntityProperty(ChatPostMessageRequest::getText, allOf(
                        containsString("*" + action + "*"),
                        containsString(contentType),
                        containsString(title))),
                RequestMatchers.requestEntityProperty(ChatPostMessageRequest::getChannel,
                        is(PUBLIC.getId()))
        ))));
    }

    private void enableNotification(String notificationKey) {
        client.admin().notifications().enable(SPACE_KEY, DUMMY_TEAM.getTeamId(), PUBLIC.getId(), notificationKey, false);
    }

    private void disableNotification(String notificationKey) {
        client.admin().notifications().disable(SPACE_KEY, DUMMY_TEAM.getTeamId(), PUBLIC.getId(), notificationKey);
    }

    private void createPage(String title) {
        executePost(contentPayload("page", title, "<p>Test content for " + title + "</p>"));
    }

    private String createPageAndGetId(String title) {
        String responseBody = executePostAndReturnBody(
                contentPayload("page", title, "<p>Test content for " + title + "</p>"));
        try {
            return new JSONObject(responseBody).getString("id");
        } catch (Exception e) {
            throw new RuntimeException("Could not parse page ID from: " + responseBody, e);
        }
    }

    private void updatePage(String pageId, String title, int version) {
        String json = "{"
                + "\"version\":{\"number\":" + version + "},"
                + "\"type\":\"page\","
                + "\"title\":\"" + title + "\","
                + "\"body\":{\"storage\":{"
                + "\"value\":\"<p>Updated content</p>\","
                + "\"representation\":\"storage\"}}"
                + "}";
        String url = confluenceBaseUrl + "/rest/api/content/" + pageId;
        try (Response response = httpClient.newCall(
                new Request.Builder().url(url).put(RequestBody.create(json, JSON)).build()
        ).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("Page update failed: HTTP " + response.code());
            }
        } catch (IOException e) {
            throw new RuntimeException("Page update request failed", e);
        }
    }

    private void createBlogPost(String title) {
        executePost(contentPayload("blogpost", title, "<p>Blog content for " + title + "</p>"));
    }

    private void executePost(String json) {
        executePostAndReturnBody(json);
    }

    private String executePostAndReturnBody(String json) {
        String url = confluenceBaseUrl + "/rest/api/content";
        try (Response response = httpClient.newCall(
                new Request.Builder().url(url).post(RequestBody.create(json, JSON)).build()
        ).execute()) {
            String body = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new RuntimeException("Content creation failed: HTTP " + response.code() + " — " + body);
            }
            return body;
        } catch (IOException e) {
            throw new RuntimeException("Content creation request failed", e);
        }
    }

    private String contentPayload(String type, String title, String htmlContent) {
        return "{\"type\":\"" + type + "\","
                + "\"title\":\"" + title + "\","
                + "\"space\":{\"key\":\"" + SPACE_KEY + "\"},"
                + "\"body\":{\"storage\":{"
                + "\"value\":\"" + htmlContent + "\","
                + "\"representation\":\"storage\"}}}";
    }
}
