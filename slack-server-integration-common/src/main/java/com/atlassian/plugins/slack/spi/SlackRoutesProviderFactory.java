package com.atlassian.plugins.slack.spi;

import com.atlassian.plugins.slack.api.routes.SlackRoutesProvider;

import java.util.Map;

/**
 * Allows the products to create a {@link com.atlassian.plugins.slack.api.routes.SlackRoutesProvider}
 * specific to the context.
 */
public interface SlackRoutesProviderFactory {
    SlackRoutesProvider getProvider(Map<String, Object> context);
}
