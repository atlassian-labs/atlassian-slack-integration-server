package com.atlassian.confluence.plugins.slack.spi.impl;

import com.atlassian.plugins.slack.api.routes.SlackRoutesProvider;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.UrlMode;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;

public class SpaceAwareSlackRoutesProvider implements SlackRoutesProvider {
    private static final String SLACK_SPACE_ADMIN = "{baseUrl}/spaces/slack2.action";

    private final String spaceKey;
    private final ApplicationProperties applicationProperties;

    SpaceAwareSlackRoutesProvider(final String spaceKey,
                                  @Qualifier("salApplicationProperties") final ApplicationProperties applicationProperties) {
        this.spaceKey = spaceKey;
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
        return UriBuilder.fromPath(SLACK_SPACE_ADMIN)
                .queryParam("key", spaceKey)
                .build(baseUrl());
    }
}
