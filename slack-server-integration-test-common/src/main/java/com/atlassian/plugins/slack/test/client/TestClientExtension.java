package com.atlassian.plugins.slack.test.client;

import com.atlassian.pageobjects.ProductInstance;
import com.atlassian.plugins.slack.test.UserCredentials;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TestClientExtension<T extends TestClient> implements BeforeAllCallback, BeforeEachCallback {
    private final UserCredentials adminCredentials;
    private final UserCredentials userCredentials;
    private final Supplier<ProductInstance> productInstanceSupplier;
    private final Function<ExtensionContext, String> testTagSupplier;
    private final BiFunction<String, UserCredentials, T> clientBuilder;
    private ProductInstance instance;
    private T adminClient;
    private T userClient;

    static {
        // enables detailed logging in failed requests to the server from the test client
        Logger.getLogger(OkHttpClient.class.getName()).setLevel(Level.FINE);
    }

    public TestClientExtension(final BiFunction<String, UserCredentials, T> clientBuilder,
                               final UserCredentials adminCredentials,
                               final UserCredentials userCredentials,
                               final Supplier<ProductInstance> productInstanceSupplier,
                               final Function<ExtensionContext, String> testTagSupplier) {
        this.clientBuilder = clientBuilder;
        this.adminCredentials = adminCredentials;
        this.userCredentials = userCredentials;
        this.productInstanceSupplier = productInstanceSupplier;
        this.testTagSupplier = testTagSupplier;
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        if (adminClient == null) {
            adminClient = clientBuilder.apply(instance().getBaseUrl(), adminCredentials);
            userClient = clientBuilder.apply(instance().getBaseUrl(), userCredentials);
        }
    }

    @Override
    public void beforeEach(final ExtensionContext context) {
        adminClient.slackLink().deleteDummyConnections();
        adminClient.setTestTag(testTagSupplier.apply(context));
        userClient.setTestTag(testTagSupplier.apply(context));
    }

    public T admin() {
        return adminClient;
    }

    public T user() {
        return userClient;
    }

    public ProductInstance instance() {
        if (instance == null) {
            instance = productInstanceSupplier.get();
        }
        return instance;
    }
}
