package com.atlassian.bitbucket.plugins.slack.spi.impl;

import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.plugins.slack.api.routes.SlackRoutesProvider;
import com.atlassian.plugins.slack.spi.SlackRoutesProviderFactory;
import com.atlassian.sal.api.ApplicationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class BitbucketSlackRoutesProviderFactory implements SlackRoutesProviderFactory {
    private final SlackRoutesProvider defaultSlackRoutesProvider;
    private final ApplicationProperties applicationProperties;

    @Autowired
    public BitbucketSlackRoutesProviderFactory(
            final SlackRoutesProvider defaultSlackRoutesProvider,
            @Qualifier("salApplicationProperties") final ApplicationProperties applicationProperties) {
        this.defaultSlackRoutesProvider = defaultSlackRoutesProvider;
        this.applicationProperties = applicationProperties;
    }

    @Override
    public SlackRoutesProvider getProvider(final Map<String, Object> context) {
        if (context.containsKey("repository")) {
            final Repository repository = (Repository) context.get("repository");
            return new RepositoryAwareSlackRoutesProvider(
                    repository.getProject().getKey(), repository.getSlug(), applicationProperties);
        }
        return defaultSlackRoutesProvider;
    }
}
