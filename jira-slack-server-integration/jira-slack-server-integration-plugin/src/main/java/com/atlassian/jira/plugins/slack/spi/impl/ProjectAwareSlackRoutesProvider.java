package com.atlassian.jira.plugins.slack.spi.impl;

import com.atlassian.plugins.slack.api.routes.SlackRoutesProvider;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.UrlMode;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;

public class ProjectAwareSlackRoutesProvider implements SlackRoutesProvider {
    private static final String SLACK_PROJECT_ADMIN = "{baseUrl}/secure/ConfigureSlack.jspa";
    private static final String SLACK_GLOBAL_ADMIN = "{baseUrl}/plugins/servlet/slack";

    private final String projectKey;
    private final ApplicationProperties applicationProperties;

    ProjectAwareSlackRoutesProvider(final String projectKey,
                                    final ApplicationProperties applicationProperties) {
        this.projectKey = projectKey;
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
        return UriBuilder.fromPath(SLACK_PROJECT_ADMIN)
                .queryParam("projectKey", projectKey)
                .build(strBaseUrl());
    }

    public URI getGlobalConfigurationPage() {
        return UriBuilder.fromPath(SLACK_GLOBAL_ADMIN)
                .path("configure")
                .build(strBaseUrl());
    };
}
