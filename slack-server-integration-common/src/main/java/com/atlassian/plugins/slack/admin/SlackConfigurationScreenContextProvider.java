package com.atlassian.plugins.slack.admin;

import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.ContextProvider;
import com.atlassian.plugins.slack.api.routes.SlackRoutesProvider;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.plugins.slack.rest.model.LimitedSlackLinkDto;
import com.atlassian.plugins.slack.spi.SlackPluginResourceProvider;
import com.atlassian.plugins.slack.spi.SlackRoutesProviderFactory;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.UrlMode;
import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public class SlackConfigurationScreenContextProvider implements ContextProvider {
    private final SlackLinkManager slackLinkManager;
    private final ApplicationProperties applicationProperties;
    private final SlackPluginResourceProvider slackPluginResourceProvider;
    private final SlackRoutesProviderFactory slackRoutesProviderFactory;

    @Autowired
    public SlackConfigurationScreenContextProvider(
            final SlackLinkManager slackLinkManager,
            @Qualifier("salApplicationProperties") final ApplicationProperties applicationProperties,
            final SlackPluginResourceProvider slackPluginResourceProvider,
            final SlackRoutesProviderFactory slackRoutesProviderFactory) {
        this.slackLinkManager = slackLinkManager;
        this.applicationProperties = applicationProperties;
        this.slackPluginResourceProvider = slackPluginResourceProvider;
        this.slackRoutesProviderFactory = slackRoutesProviderFactory;
    }

    @Override
    public void init(Map<String, String> params) throws PluginParseException {
    }

    @Override
    public Map<String, Object> getContextMap(Map<String, Object> context) {
        final ImmutableMap.Builder<String, Object> panelContext = ImmutableMap.builder();
        panelContext.putAll(context);
        panelContext.put("installedImage", slackPluginResourceProvider.getInstalledImage());
        panelContext.put("uninstallingImage", slackPluginResourceProvider.getUninstallingImage());
        panelContext.put("routes", getRoutesMap(context));
        panelContext.put("baseUrl", applicationProperties.getBaseUrl(UrlMode.ABSOLUTE));

        Collection<LimitedSlackLinkDto> links = slackLinkManager.getLinks()
                .stream()
                .map(LimitedSlackLinkDto::new)
                .collect(Collectors.toList());
        panelContext.put("links", links);
        return panelContext.build();
    }

    private Map<String, Object> getRoutesMap(Map<String, Object> context) {
        SlackRoutesProvider routes = getProvider(context);
        final ImmutableMap.Builder<String, Object> routesMap = ImmutableMap.builder();
        routesMap.put("adminConfigurationPage", routes.getAdminConfigurationPage().toString());
        routesMap.put("baseUrl", routes.baseUrl().toString());
        return routesMap.build();
    }

    private SlackRoutesProvider getProvider(Map<String, Object> context) {
        return slackRoutesProviderFactory.getProvider(context);
    }
}
