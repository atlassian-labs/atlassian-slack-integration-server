package com.atlassian.confluence.plugins.slack.spi.impl;

import com.atlassian.plugins.slack.admin.ConfigureServlet;
import com.atlassian.plugins.slack.api.routes.SlackRoutesProvider;
import com.atlassian.plugins.slack.spi.ConfigurationRedirectionManager;
import com.atlassian.plugins.slack.spi.SlackRoutesProviderFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class ConfluenceConfigurationRedirectionManager implements ConfigurationRedirectionManager {
    public static final String FROM_SPACE_ATTRIBUTE_KEY = ConfigureServlet.SLACK_EPHEMERAL_ATTRIBUTE_PREFIX + "from.space";

    public static final String SPACE_ATTRIBUTE_KEY = ConfigureServlet.SLACK_EPHEMERAL_ATTRIBUTE_PREFIX + "space.key";
    private final SlackRoutesProviderFactory routesProviderFactory;

    @Autowired
    public ConfluenceConfigurationRedirectionManager(final SlackRoutesProviderFactory routesProviderFactory) {
        this.routesProviderFactory = routesProviderFactory;
    }

    @Override
    public Optional<URI> getRedirectUri(final HttpServletRequest request) {
        final HttpSession session = request.getSession();
        final Boolean sentFromSpace = (Boolean) session.getAttribute(FROM_SPACE_ATTRIBUTE_KEY);
        if (sentFromSpace != null && sentFromSpace) {
            final String spaceKey = (String) session.getAttribute(SPACE_ATTRIBUTE_KEY);
            if (spaceKey != null) {
                final Map<String, Object> context = new HashMap<>();
                context.put("spaceKey", spaceKey);
                final SlackRoutesProvider routesProvider = routesProviderFactory.getProvider(context);
                return Optional.of(routesProvider.getAdminConfigurationPage());
            }
        }

        return Optional.empty();
    }
}
