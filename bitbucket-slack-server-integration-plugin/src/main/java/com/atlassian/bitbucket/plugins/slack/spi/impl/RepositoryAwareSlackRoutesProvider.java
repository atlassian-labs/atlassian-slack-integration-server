package com.atlassian.bitbucket.plugins.slack.spi.impl;

import com.atlassian.plugins.slack.api.routes.SlackRoutesProvider;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.UrlMode;
import org.springframework.beans.factory.annotation.Qualifier;

import jakarta.ws.rs.core.UriBuilder;
import java.net.URI;

public class RepositoryAwareSlackRoutesProvider implements SlackRoutesProvider {
    private static final String SLACK_REPO_ADMIN = "{baseUrl}/plugins/servlet/repo-slack-settings/projects/{key}/repos/{slug}";

    private final String key;
    private final String slug;
    private final ApplicationProperties applicationProperties;

    RepositoryAwareSlackRoutesProvider(
            final String key,
            final String slug,
            @Qualifier("salApplicationProperties") final ApplicationProperties applicationProperties) {
        this.key = key;
        this.slug = slug;
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
        return UriBuilder.fromPath(SLACK_REPO_ADMIN).build(baseUrl(), key, slug);
    }
}
