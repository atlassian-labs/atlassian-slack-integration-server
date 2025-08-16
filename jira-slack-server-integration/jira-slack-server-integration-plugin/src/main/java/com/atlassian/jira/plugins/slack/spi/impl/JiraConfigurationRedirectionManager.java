package com.atlassian.jira.plugins.slack.spi.impl;

import com.atlassian.plugins.slack.admin.ConfigureServlet;
import com.atlassian.plugins.slack.api.routes.SlackRoutesProvider;
import com.atlassian.plugins.slack.spi.ConfigurationRedirectionManager;
import com.atlassian.plugins.slack.spi.SlackRoutesProviderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.springframework.web.util.WebUtils.getSessionAttribute;

@Component("jiraConfigurationRedirectionManager")
public class JiraConfigurationRedirectionManager implements ConfigurationRedirectionManager {
    public static final String FROM_PROJECT_ATTRIBUTE_KEY = ConfigureServlet.SLACK_EPHEMERAL_ATTRIBUTE_PREFIX + "from.project";

    public static final String PROJECT_ATTRIBUTE_KEY = ConfigureServlet.SLACK_EPHEMERAL_ATTRIBUTE_PREFIX + "project.key";
    private final SlackRoutesProviderFactory routesProviderFactory;

    @Autowired
    public JiraConfigurationRedirectionManager(final SlackRoutesProviderFactory routesProviderFactory) {
        this.routesProviderFactory = routesProviderFactory;
    }

    @Override
    public Optional<URI> getRedirectUri(final HttpServletRequest request) {
        final Boolean sentFromProject = (Boolean) getSessionAttribute(request, FROM_PROJECT_ATTRIBUTE_KEY);
        if (sentFromProject != null && sentFromProject) {
            final String projectKey = (String) getSessionAttribute(request, PROJECT_ATTRIBUTE_KEY);
            if (projectKey != null) {
                final Map<String, Object> context = new HashMap<>();
                context.put("projectKey", projectKey);
                final SlackRoutesProvider routesProvider = routesProviderFactory.getProvider(context);
                return Optional.of(routesProvider.getAdminConfigurationPage());
            }
        }

        return Optional.empty();
    }
}
