package com.atlassian.plugins.slack.api.routes;

import java.net.URI;

/**
 * Routes to REST resources provided by the Slack Plugin
 */
public interface SlackRoutesProvider {
    URI getAdminConfigurationPage();

    URI baseUrl();
}
