package com.atlassian.confluence.plugins.slack.spi.impl;

import com.atlassian.confluence.spaces.Space;
import com.atlassian.plugins.slack.api.routes.SlackRoutesProvider;
import com.atlassian.plugins.slack.spi.SlackRoutesProviderFactory;
import com.atlassian.sal.api.ApplicationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ConfluenceSlackRoutesProviderFactory implements SlackRoutesProviderFactory {
    private final SlackRoutesProvider defaultSlackRoutesProvider;
    private final ApplicationProperties applicationProperties;

    @Autowired
    public ConfluenceSlackRoutesProviderFactory(
            final SlackRoutesProvider defaultSlackRoutesProvider,
            @Qualifier("salApplicationProperties") final ApplicationProperties applicationProperties) {
        this.defaultSlackRoutesProvider = defaultSlackRoutesProvider;
        this.applicationProperties = applicationProperties;
    }

    @Override
    public SlackRoutesProvider getProvider(final Map<String, Object> context) {
        if (context.containsKey("spaceKey")) {
            return new SpaceAwareSlackRoutesProvider((String) context.get("spaceKey"), applicationProperties);
        } else if (context.containsKey("space")) {
            final Space space = (Space) context.get("space");
            return new SpaceAwareSlackRoutesProvider(space.getKey(), applicationProperties);
        }

        return defaultSlackRoutesProvider;
    }
}
