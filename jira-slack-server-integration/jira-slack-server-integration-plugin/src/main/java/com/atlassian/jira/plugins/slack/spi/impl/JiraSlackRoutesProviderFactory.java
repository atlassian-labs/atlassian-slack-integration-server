package com.atlassian.jira.plugins.slack.spi.impl;

import com.atlassian.plugins.slack.api.routes.SlackRoutesProvider;
import com.atlassian.plugins.slack.spi.SlackRoutesProviderFactory;
import com.atlassian.sal.api.ApplicationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component("slackRoutesProviderFactory")
public class JiraSlackRoutesProviderFactory implements SlackRoutesProviderFactory {
    private final SlackRoutesProvider defaultSlackRoutesProvider;
    private final ApplicationProperties applicationProperties;

    @Autowired
    public JiraSlackRoutesProviderFactory(
            final SlackRoutesProvider defaultSlackRoutesProvider,
            @Qualifier("salApplicationProperties") final ApplicationProperties applicationProperties) {
        this.defaultSlackRoutesProvider = defaultSlackRoutesProvider;
        this.applicationProperties = applicationProperties;
    }

    @Override
    public SlackRoutesProvider getProvider(final Map<String, Object> context) {
        final String projectKey = (String) context.get("projectKey");
        if (projectKey != null) {
            return new ProjectAwareSlackRoutesProvider(projectKey, applicationProperties);
        }

        return defaultSlackRoutesProvider;
    }
}
