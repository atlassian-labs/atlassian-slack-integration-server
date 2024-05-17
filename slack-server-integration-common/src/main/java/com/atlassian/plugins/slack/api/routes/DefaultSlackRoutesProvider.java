package com.atlassian.plugins.slack.api.routes;

import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.UrlMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Arrays;

@Component("slackRoutesProvider")
public class DefaultSlackRoutesProvider implements SlackRoutesProvider {
    private static final String SLACK_ADMIN_BASE = "{baseUrl}/plugins/servlet/slack";

    private final ApplicationProperties applicationProperties;

    @Autowired
    public DefaultSlackRoutesProvider(@Qualifier("salApplicationProperties") ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    private String strBaseUrl() {
        return applicationProperties.getBaseUrl(UrlMode.ABSOLUTE);
    }

    @Override
    public URI baseUrl() {
        return URI.create(strBaseUrl());
    }

    @Override
    public URI getAdminConfigurationPage() {
        return UriBuilder.fromPath(SLACK_ADMIN_BASE)
                .path("configure")
                .build(new Object[]{strBaseUrl()}, false);
    }
}
