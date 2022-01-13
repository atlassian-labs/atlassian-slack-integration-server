package com.atlassian.plugins.slack.api.client;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.SlackUser;
import com.atlassian.plugins.slack.api.client.cache.SlackResponseCache;
import com.atlassian.plugins.slack.api.client.interceptor.BackoffRetryInterceptor;
import com.atlassian.plugins.slack.api.client.interceptor.RateLimitRetryInterceptor;
import com.atlassian.plugins.slack.api.client.interceptor.RequestIdInterceptor;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.plugins.slack.user.SlackUserManager;
import com.atlassian.plugins.slack.util.ErrorResponse;
import com.atlassian.sal.api.user.UserManager;
import com.github.seratch.jslack.Slack;
import com.github.seratch.jslack.api.methods.MethodsClient;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import com.github.seratch.jslack.api.methods.response.chat.ChatPostMessageResponse;
import com.github.seratch.jslack.api.model.Message;
import com.github.seratch.jslack.common.http.SlackHttpClient;
import com.github.seratch.jslack.common.json.GsonFactory;
import com.google.gson.Gson;
import io.atlassian.fugue.Either;
import io.atlassian.fugue.Pair;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.concurrent.Callable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@PrepareForTest({DefaultSlackClientProvider.class, BackoffRetryInterceptor.class, RateLimitRetryInterceptor.class})
@PowerMockIgnore("javax.net.ssl.*")
@RunWith(PowerMockRunner.class)
public class DefaultSlackClientWithMockedServerTest {
    private static final String TEAM_ID = DefaultSlackClient.TEAM_DUMMY_PREFIX + "someTeamId";
    private static final String USER_TOKEN = "someUserToken";
    private static final String BOT_TOKEN = "someBotToken";
    private static final String CLIENT_ID = "someClientId";
    private static final String CLIENT_SECRET = "someClientSecret";

    private final Gson gson = GsonFactory.createSnakeCase();

    @Mock
    private SlackHttpClient slackHttpClient;
    @Mock
    private Slack slack;
    @Mock
    private SlackLink slackLink;
    @Mock
    private SlackLinkManager slackLinkManager;
    @Mock
    private SlackUser slackUser;
    @Mock
    private SlackUserManager slackUserManager;
    @Mock
    private UserManager userManager;
    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private SlackResponseCache slackResponseCache;
    @Mock
    private MethodsClient methods;
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private SlackClient client;
    private MockWebServer server;

    @Before
    public void setUp() throws InterruptedException {
        PowerMockito.mockStatic(Thread.class);
        PowerMockito.doNothing().when(Thread.class);
        Thread.sleep(anyLong());

        when(slackLink.getTeamId()).thenReturn(TEAM_ID);
        when(slackLink.getClientId()).thenReturn(CLIENT_ID);
        when(slackLink.getClientSecret()).thenReturn(CLIENT_SECRET);
        when(slackLink.getBotAccessToken()).thenReturn(BOT_TOKEN);
        when(slack.methods()).thenReturn(methods);
        when(slackUser.getUserToken()).thenReturn(USER_TOKEN);
        final DefaultSlackClientProvider provider = new DefaultSlackClientProvider(slackLinkManager, slackUserManager,
                userManager, eventPublisher, new ExecutorServiceHelper(), new RequestIdInterceptor(),
                new BackoffRetryInterceptor(), new RateLimitRetryInterceptor(), slackResponseCache);
        client = provider.withLink(slackLink);
    }

    @Test
    public void postMessage_shouldRetryAfterRateLimit() {
        withHttpServer(() -> {
            final Pair<ChatPostMessageRequest, ChatPostMessageResponse> messageAndResponse = getMessageAndResponse();

            server.enqueue(new MockResponse().setResponseCode(429).addHeader("Retry-After", "1"));
            server.enqueue(new MockResponse().setResponseCode(429).addHeader("Retry-After", "2"));
            server.enqueue(new MockResponse().setResponseCode(200).setBody(gson.toJson(messageAndResponse.right())));
            server.start(9753);

            Either<ErrorResponse, Message> response = client.postMessage(messageAndResponse.left());

            assertTrue(response.isRight());
            assertEquals("Response is correct", messageAndResponse.right().getMessage(), response.getOrNull());

            assertEquals("number of retry request", 3, server.getRequestCount());

            final RecordedRequest req1 = server.takeRequest();
            assertNotNull("First retry header", req1.getHeader(RequestIdInterceptor.REQ_ID_HEADER));

            verifySleptFor(1000L);

            final RecordedRequest req2 = server.takeRequest();
            assertNotNull("Second retry header", req2.getHeader(RequestIdInterceptor.REQ_ID_HEADER));

            verifySleptFor(2000L);

            final RecordedRequest req = server.takeRequest();
            assertNotNull("Final request header", req.getHeader(RequestIdInterceptor.REQ_ID_HEADER));

            return null;
        });
    }


    @Test
    public void postMessage_shouldRetryWithBackOff() {
        withHttpServer(() -> {
            final Pair<ChatPostMessageRequest, ChatPostMessageResponse> messageAndResponse = getMessageAndResponse();

            server.enqueue(new MockResponse().setResponseCode(503));
            server.enqueue(new MockResponse().setResponseCode(500));
            server.enqueue(new MockResponse().setResponseCode(200).setBody(gson.toJson(messageAndResponse.right())));
            server.start(9753);

            Either<ErrorResponse, Message> response = client.postMessage(messageAndResponse.left());

            assertTrue(response.isRight());
            assertEquals("Response is correct", messageAndResponse.right().getMessage(), response.getOrNull());

            assertEquals("number of retry request", 3, server.getRequestCount());

            final RecordedRequest req1 = server.takeRequest();
            assertNotNull("First retry header", req1.getHeader(RequestIdInterceptor.REQ_ID_HEADER));

            verifySleptFor(1000L);

            final RecordedRequest req2 = server.takeRequest();
            assertNotNull("Second retry header", req2.getHeader(RequestIdInterceptor.REQ_ID_HEADER));

            verifySleptFor(3000L);

            final RecordedRequest req = server.takeRequest();
            assertNotNull("Final request header", req.getHeader(RequestIdInterceptor.REQ_ID_HEADER));

            return null;
        });
    }

    private void verifySleptFor(long ms) throws InterruptedException {
        PowerMockito.verifyStatic(Thread.class);
        Thread.sleep(ms);
    }

    private <T> T withHttpServer(final Callable<T> test) {
        server = new MockWebServer();
        try {
            return test.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                server.shutdown();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private Pair<ChatPostMessageRequest, ChatPostMessageResponse> getMessageAndResponse() {
        ChatPostMessageRequest req = ChatPostMessageRequest.builder()
                .channel("someChannelId")
                .text("someText")
                .mrkdwn(true)
                .build();

        final ChatPostMessageResponse resp = new ChatPostMessageResponse();
        resp.setChannel("someChanelId");
        resp.setOk(true);
        resp.setTs("123");
        final Message msg = new Message();
        msg.setText("someText");
        resp.setMessage(msg);

        return Pair.pair(req, resp);
    }
}
