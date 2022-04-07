package com.atlassian.plugins.slack.api.client;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugins.slack.api.SlackLinkDto;
import com.atlassian.plugins.slack.api.client.cache.SlackResponseCache;
import com.atlassian.plugins.slack.api.client.interceptor.BackoffRetryInterceptor;
import com.atlassian.plugins.slack.api.client.interceptor.RateLimitRetryInterceptor;
import com.atlassian.plugins.slack.api.client.interceptor.RequestIdInterceptor;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.plugins.slack.user.SlackUserManager;
import com.atlassian.sal.api.user.UserManager;
import io.atlassian.fugue.Either;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.ProxySelector;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultSlackClientProviderTest {
    @Mock
    private SlackLinkManager slackLinkManager;
    @Mock
    private SlackUserManager slackUserManager;
    @Mock
    private UserManager userManager;
    @Mock
    private ExecutorServiceHelper executorServiceHelper;
    @Mock
    private ExecutorService executorService;
    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private SlackResponseCache slackResponseCache;
    @Mock
    private ProxySelector proxySelector;

    private RequestIdInterceptor requestIdInterceptor = new RequestIdInterceptor();
    private BackoffRetryInterceptor backoffRetryInterceptor = new BackoffRetryInterceptor();
    private RateLimitRetryInterceptor rateLimitRetryInterceptor = new RateLimitRetryInterceptor();

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private DefaultSlackClientProvider provider;
    private MockWebServer server;

    @Before
    public void setUp() {
        provider = spy(new DefaultSlackClientProvider(slackLinkManager, slackUserManager, userManager, eventPublisher,
                executorServiceHelper, requestIdInterceptor, backoffRetryInterceptor, rateLimitRetryInterceptor,
                slackResponseCache));
        when(executorServiceHelper.createBoundedExecutorService()).thenReturn(executorService);
    }

    @After
    public void tearDown() {
        provider.destroy();
    }

    @Test
    public void withLink_shouldCreateClientWithProvidedLink() {
        SlackLinkDto link = new SlackLinkDto();

        SlackClient client = provider.withLink(link);

        assertThat(client.getLink(), is(sameInstance(link)));
    }

    @Test
    public void withTeamId_shouldCreateClientWithExtractedLink() {
        String teamId = "someTeamId";
        SlackLinkDto link = new SlackLinkDto();
        when(slackLinkManager.getLinkByTeamId(teamId)).thenReturn(Either.right(link));

        Either<Throwable, SlackClient> client = provider.withTeamId(teamId);

        assertThat(client.isRight(), is(true));
        assertThat(client.getOrNull().getLink(), is(sameInstance(link)));
    }

    @Test
    public void withoutCredentials_shouldCreateRestrictedClient() {
        SlackLimitedClient client = provider.withoutCredentials();

        assertThat(client, Matchers.instanceOf(DefaultSlackLimitedClient.class));
    }

    @Test
    public void destroy_shouldShutDownExecutorService_whenItWasInitialized() {
        provider.withoutCredentials();
        provider.destroy();

        verify(executorService).shutdown();
    }

    @Test
    public void client_shouldAuthenticateWithHttpProxy() {
        withProxyServer(() -> {
            server.enqueue(new MockResponse().setResponseCode(HttpURLConnection.HTTP_PROXY_AUTH));
            server.enqueue(new MockResponse().setResponseCode(200));
            server.start();

            System.setProperty("http.proxyUser", "user");
            System.setProperty("http.proxyPassword", "pass");
            ProxySelector.setDefault(proxySelector);
            when(proxySelector.select(any())).thenReturn(Collections.singletonList(server.toProxyAddress()));

            OkHttpClient okHttpClient = provider.new OkHttpClientLazyReference().get();
            final Response response = okHttpClient.newCall(new Request.Builder().url("http://example.com").get().build()).execute();

            assertThat(response.code(), is(200));

            RecordedRequest nonAuthorizedRequest = server.takeRequest();
            assertThat(nonAuthorizedRequest.getHeader("Proxy-Authorization"), nullValue());

            RecordedRequest authorizedRequest = server.takeRequest();
            assertThat(authorizedRequest.getHeader("Proxy-Authorization"), is(Credentials.basic("user", "pass")));

            server.shutdown();

            return null;
        });
    }

    @Test
    public void client_shouldAuthenticateWithHttpsProxyUsingPreemptiveAuthentication() {
        withProxyServer(() -> {
            SSLContext sslContext = new SslContextBuilder(InetAddress.getLocalHost().getHostName()).build();
            SSLSocketFactory socketFactory = sslContext.getSocketFactory();
            server.useHttps(socketFactory, true);
            server.enqueue(new MockResponse().setResponseCode(200).setSocketPolicy(SocketPolicy.UPGRADE_TO_SSL_AT_END));
            server.enqueue(new MockResponse().setResponseCode(200));
            server.start();

            System.setProperty("https.proxyUser", "user");
            System.setProperty("https.proxyPassword", "pass");
            ProxySelector.setDefault(proxySelector);
            when(proxySelector.select(any())).thenReturn(Collections.singletonList(server.toProxyAddress()));

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init((KeyStore) null);
            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
            if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
                throw new IllegalStateException("Unexpected default trust managers:"
                        + Arrays.toString(trustManagers));
            }
            X509TrustManager trustManager = (X509TrustManager) trustManagers[0];

            OkHttpClient okHttpClient = provider.new OkHttpClientLazyReference().get()
                    .newBuilder()
                    .hostnameVerifier((s, sslSession) -> true)
                    .sslSocketFactory(socketFactory, trustManager)
                    .build();
            final Response response = okHttpClient.newCall(new Request.Builder().url("https://example.com/test").get().build()).execute();

            assertThat(response.code(), is(200));

            RecordedRequest authorizedConnectRequest = server.takeRequest();
            assertThat("Connect line failure on proxy", authorizedConnectRequest.getRequestLine(), is("CONNECT example.com:443 HTTP/1.1"));
            assertThat(authorizedConnectRequest.getHeader("Proxy-Authorization"), is(Credentials.basic("user", "pass")));

            RecordedRequest authorizedGetRequest = server.takeRequest();
            assertThat("Connect line failure on proxy", authorizedGetRequest.getRequestLine(), is("GET /test HTTP/1.1"));
            assertThat(authorizedGetRequest.getHeader("Proxy-Authorization"), nullValue());

            return null;
        });
    }

    private void withProxyServer(Callable test) {
        String httpProxyHost = System.clearProperty("http.proxyHost");
        String httpProxyPort = System.clearProperty("http.proxyPort");
        String httpProxyUser = System.clearProperty("http.proxyUser");
        String httpProxyPassword = System.clearProperty("http.proxyPassword");
        String httpsProxyUser = System.clearProperty("https.proxyUser");
        String httpsProxyPassword = System.clearProperty("https.proxyPassword");
        String httpsProxyHost = System.clearProperty("https.proxyHost");
        String httpsProxyPort = System.clearProperty("https.proxyPort");
        ProxySelector oldProxySelector = ProxySelector.getDefault();
        server = new MockWebServer();
        try {
            test.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            restoreProperty("http.proxyHost", httpProxyHost);
            restoreProperty("http.proxyPort", httpProxyPort);
            restoreProperty("http.proxyUser", httpProxyUser);
            restoreProperty("http.proxyPassword", httpProxyPassword);
            restoreProperty("https.proxyUser", httpsProxyUser);
            restoreProperty("https.proxyPassword", httpsProxyPassword);
            restoreProperty("https.proxyHost", httpsProxyHost);
            restoreProperty("https.proxyPort", httpsProxyPort);
            ProxySelector.setDefault(oldProxySelector);
            try {
                server.shutdown();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void restoreProperty(String name, String oldValue) {
        if (oldValue == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, oldValue);
        }
    }
}
