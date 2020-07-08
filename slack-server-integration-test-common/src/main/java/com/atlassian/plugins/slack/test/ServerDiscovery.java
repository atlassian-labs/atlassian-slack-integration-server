package com.atlassian.plugins.slack.test;

import com.atlassian.pageobjects.DefaultProductInstance;
import com.atlassian.pageobjects.Defaults;
import com.atlassian.pageobjects.TestedProduct;
import com.atlassian.plugins.slack.test.client.TestClient;
import com.atlassian.webdriver.pageobjects.WebDriverTester;
import lombok.experimental.UtilityClass;
import okhttp3.HttpUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@UtilityClass
public class ServerDiscovery {
    public static DefaultProductInstance instance(final String instanceId,
                                           final Class<? extends TestedProduct<WebDriverTester>> testedProductClass,
                                           final UserCredentials adminCredentials) {
        return instance(instanceId, testedProductClass, adminCredentials, Optional.empty());
    }

    public static DefaultProductInstance instance(final String instanceId,
                                           final Class<? extends TestedProduct<WebDriverTester>> testedProductClass,
                                           final UserCredentials adminCredentials,
                                           final Optional<Integer> defaultPortOverride) {
        final Logger log = LoggerFactory.getLogger(ServerDiscovery.class);

        final HttpUrl baseUrl;
        final String ampsBaseUrl = System.getProperty("baseurl." + instanceId);

        Defaults defaults = testedProductClass.getAnnotation(Defaults.class);
        if (ampsBaseUrl != null) {
            log.info("Obtained base URL from AMPS system property at {}", ampsBaseUrl);
            baseUrl = HttpUrl.get(ampsBaseUrl);
        } else {
            baseUrl = HttpUrl.get("http://localhost:" + defaultPortOverride.orElse(defaults.httpPort()) + defaults.contextPath());
            log.info("Obtained default base URL from class {} at {}", testedProductClass, baseUrl);
        }

        final TestClient discoveryClient = new TestClient(baseUrl.toString(), adminCredentials);
        final HttpUrl realBaseUrl = discoveryClient.getRedirectBaseUrl().orElse(baseUrl);
        if (!realBaseUrl.equals(baseUrl)) {
            log.info("Obtained real base URL from server: {}", realBaseUrl);
        }

        return new DefaultProductInstance(realBaseUrl.toString(), instanceId, realBaseUrl.port(), realBaseUrl.encodedPath());
    }
}
