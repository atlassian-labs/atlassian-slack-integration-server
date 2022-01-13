package com.atlassian.plugins.slack.api.client;

import com.atlassian.annotations.VisibleForTesting;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.client.cache.SlackResponseCache;
import com.atlassian.plugins.slack.api.client.interceptor.BackoffRetryInterceptor;
import com.atlassian.plugins.slack.api.client.interceptor.RateLimitRetryInterceptor;
import com.atlassian.plugins.slack.api.client.interceptor.RequestIdInterceptor;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.plugins.slack.user.SlackUserManager;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.util.concurrent.LazyReference;
import com.github.seratch.jslack.Slack;
import com.github.seratch.jslack.common.http.SlackHttpClient;
import io.atlassian.fugue.Either;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Authenticator;
import okhttp3.Challenge;
import okhttp3.Credentials;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
@Component
public class DefaultSlackClientProvider implements SlackClientProvider, DisposableBean {
    private static final int DEFAULT_CONNECT_TIMEOUT_MILLISECONDS = 3 * 1000;
    // https://pi-dev-sandbox.atlassian.net/browse/JSS-8
    // read timeout cannot be less than 12 (or event 15 for sure) seconds to not trigger message duplicates
    // 1. if Slack message being sent has images, Slack tries to load them for at most ~10-12 seconds
    // 2a. for BlockKit messages `invalid_blocks` error is returned, message isn't sent
    // 2b. for legacy messages (like in Jira plugin) message is sent without images and errors after 10-12 seconds
    // if timeout is less than the time during which Slack tries to load an image, exception is thrown,
    // BackoffRetryInterceptor performs the request again, with the same result causing up to 8 duplicates
    private static final int DEFAULT_READ_TIMEOUT_MILLISECONDS = 15 * 1000;
    private static final int DEFAULT_WRITE_TIMEOUT_MILLISECONDS = 120 * 1000;

    private final SlackLinkManager slackLinkManager;
    private final SlackUserManager slackUserManager;
    private final UserManager userManager;
    private final EventPublisher eventPublisher;

    private final boolean retryOnConnectionFailure;
    private final Integer connectionTimeout;
    private final Integer readTimeout;
    private final Integer writeTimeout;
    private final Integer pingInterval;
    private final boolean forceHttp1;
    private final ExecutorServiceHelper executorServiceHelper;
    private final RequestIdInterceptor requestIdInterceptor;
    private final BackoffRetryInterceptor backoffRetryInterceptor;
    private final RateLimitRetryInterceptor rateLimitRetryInterceptor;
    private final SlackResponseCache slackResponseCache;

    private LazyReference<OkHttpClient> client = new OkHttpClientLazyReference();

    @Autowired
    public DefaultSlackClientProvider(final SlackLinkManager slackLinkManager,
                                      final SlackUserManager slackUserManager,
                                      @Qualifier("salUserManager") final UserManager userManager,
                                      final EventPublisher eventPublisher,
                                      final ExecutorServiceHelper executorServiceHelper,
                                      final RequestIdInterceptor requestIdInterceptor,
                                      final BackoffRetryInterceptor backoffRetryInterceptor,
                                      final RateLimitRetryInterceptor rateLimitRetryInterceptor,
                                      final SlackResponseCache slackResponseCache) {
        this.slackLinkManager = slackLinkManager;
        this.slackUserManager = slackUserManager;
        this.userManager = userManager;
        this.eventPublisher = eventPublisher;
        this.executorServiceHelper = executorServiceHelper;
        this.requestIdInterceptor = requestIdInterceptor;
        this.backoffRetryInterceptor = backoffRetryInterceptor;
        this.rateLimitRetryInterceptor = rateLimitRetryInterceptor;
        this.slackResponseCache = slackResponseCache;

        retryOnConnectionFailure = Boolean.valueOf(System.getProperty("slack.client.retry.on.connection.failure", "true"));
        connectionTimeout = Integer.getInteger("slack.client.connect.timeout", DEFAULT_CONNECT_TIMEOUT_MILLISECONDS);
        readTimeout = Integer.getInteger("slack.client.read.timeout", DEFAULT_READ_TIMEOUT_MILLISECONDS);
        writeTimeout = Integer.getInteger("slack.client.write.timeout", DEFAULT_WRITE_TIMEOUT_MILLISECONDS);
        pingInterval = Integer.getInteger("slack.client.ping.interval", 0);
        forceHttp1 = Boolean.valueOf(System.getProperty("slack.client.force.http1", "false"));
    }

    @Override
    public SlackClient withLink(final SlackLink link) {
        checkNotNull(link, "link cannot be null");
        return new DefaultSlackClient(new SlackHttpClient(
                client.get()), link, slackUserManager, userManager, eventPublisher, slackResponseCache);
    }

    @Override
    public Either<Throwable, SlackClient> withTeamId(final String teamId) {
        return slackLinkManager
                .getLinkByTeamId(teamId)
                .map(link -> new DefaultSlackClient(new SlackHttpClient(
                        client.get()), link, slackUserManager, userManager, eventPublisher, slackResponseCache));
    }

    @Override
    public SlackLimitedClient withoutCredentials() {
        return new DefaultSlackLimitedClient(Slack.getInstance(new SlackHttpClient(client.get())));
    }

    @Override
    public void destroy() {
        if (client.isInitialized()) {
            try {
                Objects.requireNonNull(client.get()).dispatcher().executorService().shutdown();
            } catch (Throwable e) {
                // no-op
            }
        }
    }

    @VisibleForTesting
    protected class OkHttpClientLazyReference extends LazyReference<OkHttpClient> {
        private Authenticator createProxyAuthenticator() {
            String httpProxyUser = System.getProperty("http.proxyUser");
            String httpProxyPassword = System.getProperty("http.proxyPassword");
            String httpCredentials = StringUtils.isNotBlank(httpProxyUser)
                    ? Credentials.basic(httpProxyUser, httpProxyPassword)
                    : null;

            String httpsProxyUser = System.getProperty("https.proxyUser");
            String httpsProxyPassword = System.getProperty("https.proxyPassword");
            String httpsCredentials = StringUtils.isNotBlank(httpsProxyUser)
                    ? Credentials.basic(httpsProxyUser, httpsProxyPassword)
                    : null;

            if (log.isDebugEnabled()) {
                Map<String, String> map = new HashMap<>();
                map.put("httpProxyUser", httpProxyUser);
                map.put("httpProxyPassword", Boolean.toString(httpProxyPassword != null && httpProxyPassword.length() > 0));
                map.put("httpCredentials", Boolean.toString(httpCredentials != null && httpCredentials.length() > 0));
                map.put("httpsProxyUser", httpsProxyUser);
                map.put("httpsProxyPassword", Boolean.toString(httpsProxyPassword != null && httpsProxyPassword.length() > 0));
                map.put("httpsCredentials", Boolean.toString(httpsCredentials != null && httpsCredentials.length() > 0));
                log.debug("OkHttpClientLazyReference instantiated with options {}", map);
            }

            return (route, response) -> {
                final String credentials = "https".equals(route.address().url().scheme()) ? httpsCredentials : httpCredentials;
                if (credentials == null) {
                    return null;
                }

                for (Challenge challenge : response.challenges()) {
                    // If this is preemptive auth, use a preemptive credential.
                    if (challenge.scheme().equalsIgnoreCase("OkHttp-Preemptive")) {
                        return response.request().newBuilder()
                                .header("Proxy-Authorization", credentials)
                                .build();
                    }
                }

                if (response.request().header("Proxy-Authorization") != null) {
                    return null; // Give up, we've already failed to authenticate.
                }

                return response.request().newBuilder()
                        .header("Proxy-Authorization", credentials)
                        .build();
            };
        }

        @Override
        protected OkHttpClient create() {
            final OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    .dispatcher(new Dispatcher(executorServiceHelper.createBoundedExecutorService()))
                    .addInterceptor(requestIdInterceptor)
                    .addInterceptor(backoffRetryInterceptor)
                    .addInterceptor(rateLimitRetryInterceptor)
                    .retryOnConnectionFailure(retryOnConnectionFailure)
                    .connectTimeout(connectionTimeout, TimeUnit.MILLISECONDS)
                    .readTimeout(readTimeout, TimeUnit.MILLISECONDS)
                    .writeTimeout(writeTimeout, TimeUnit.MILLISECONDS)
                    .proxyAuthenticator(createProxyAuthenticator())
                    // https://square.github.io/okhttp/4.x/okhttp/okhttp3/-ok-http-client/-builder/ping-interval/
                    .pingInterval(pingInterval, TimeUnit.MILLISECONDS);
            if (forceHttp1) {
                builder.protocols(Collections.singletonList(Protocol.HTTP_1_1));
            }


            final OkHttpClient client = builder.build();
            if (log.isDebugEnabled()) {
                Map<String, String> map = new HashMap<>();
                map.put("forceHttp1", Boolean.toString(forceHttp1));
                map.put("retryOnConnectionFailure", Boolean.toString(retryOnConnectionFailure));
                map.put("connectionTimeout", Integer.toString(connectionTimeout));
                map.put("readTimeout", Integer.toString(readTimeout));
                map.put("writeTimeout", Integer.toString(writeTimeout));
                map.put("pingInterval", Integer.toString(pingInterval));
                log.debug("OkHttpClient instantiated with options {}", map);
            }

            return client;
        }
    }
}
