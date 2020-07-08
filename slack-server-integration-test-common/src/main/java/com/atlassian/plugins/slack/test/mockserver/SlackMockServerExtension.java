package com.atlassian.plugins.slack.test.mockserver;

import com.atlassian.pageobjects.elements.query.Conditions;
import com.atlassian.pageobjects.elements.query.Poller;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.atlassian.plugins.slack.api.client.DefaultSlackClient.SLACK_MOCK_SERVER_DEFAULT_BASE_URL;
import static com.atlassian.plugins.slack.api.client.DefaultSlackClient.SLACK_MOCK_SERVER_SYSTEM_PROPERTY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

@Slf4j
public class SlackMockServerExtension implements BeforeAllCallback, BeforeEachCallback, ExtensionContext.Store.CloseableResource {
    private static final int ASYNC_ASSERTION_TIMEOUT = 10_000;
    private final Supplier<Set<String>> baseUrlsSupplier;
    private final Function<ExtensionContext, String> testTagSupplier;
    private SlackMockServer server;

    /**
     * Lazy-loads the server details to conform to the test lifecycle
     */
    public SlackMockServerExtension(final Supplier<Set<String>> baseUrlsSupplier,
                                    final Function<ExtensionContext, String> testTagSupplier) {
        this.baseUrlsSupplier = baseUrlsSupplier;
        this.testTagSupplier = testTagSupplier;
    }

    @Override
    public void beforeAll(final ExtensionContext context) {
        if (server == null) {
            final Set<String> baseUrls = baseUrlsSupplier.get();
            server = new SlackMockServer(baseUrls);

            final URI uri = URI.create(System.getProperty(SLACK_MOCK_SERVER_SYSTEM_PROPERTY, SLACK_MOCK_SERVER_DEFAULT_BASE_URL));
            server.start(uri.getHost(), uri.getPort());

            // registers a callback when the root test context is shut down
            if (context != null) {
                context.getRoot().getStore(GLOBAL).put(SlackMockServerExtension.class.getName(), this);
            }
        }
    }

    @Override
    public void beforeEach(final ExtensionContext context) {
        server.setTestTag(testTagSupplier.apply(context));
        server.clearRecords();
    }

    public List<RequestHistoryItem> requestHistoryForTest() {
        return server.requestHistoryForTest();
    }

    public void clearHistoryExecuteAndWaitForNewRequest(final String requestType, final Runnable run) {
        server.clearRecords();
        run.run();
        Poller.waitUntilTrue(
                "Expected request to " + requestType,
                Conditions.forSupplier(ASYNC_ASSERTION_TIMEOUT, () -> !server.requestHistoryForTest(requestType).isEmpty()));
    }

    public <T> T clearHistoryExecuteAndWaitForNewRequest(final String requestType, final Supplier<T> run) {
        server.clearRecords();
        final T result = run.get();
        Poller.waitUntilTrue(
                "Expected request to " + requestType,
                Conditions.forSupplier(ASYNC_ASSERTION_TIMEOUT, () -> !server.requestHistoryForTest(requestType).isEmpty()));
        return result;
    }

    /**
     * This basically waits a couple of seconds to make sure we didn't send any notifications.
     * Of course this isn't the most reliable way but we're pending coming up a better strategy.
     */
    public void clearHistoryExecuteAndExpectNoRequests(final String requestType, final Runnable run) {
        server.clearRecords();
        run.run();
        try {
            log.info("Waiting {}ms for requests to Slack in order to make sure we're not missing any of them...", ASYNC_ASSERTION_TIMEOUT);
            Thread.sleep(ASYNC_ASSERTION_TIMEOUT);
        } catch (InterruptedException e) {
            // no-op
        }
        assertThat("Unexpected request to " + requestType, server.requestHistoryForTest(requestType), empty());
    }

    @Override
    public void close() {
        if (server != null) {
            try {
                server.shutdown();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
