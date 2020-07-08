package com.atlassian.bitbucket.plugins.slack.provider;

import com.atlassian.bitbucket.plugins.slack.notification.configuration.NotificationConfigurationContextBuilder;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.ContextProvider;
import com.atlassian.plugins.slack.api.SlackLink;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

public class SlackGlobalAdminDataProvider implements ContextProvider {
    private final NotificationConfigurationContextBuilder notificationConfigurationContextBuilder;

    SlackGlobalAdminDataProvider(final NotificationConfigurationContextBuilder notificationConfigurationContextBuilder) {
        this.notificationConfigurationContextBuilder = notificationConfigurationContextBuilder;
    }

    @Override
    public void init(final Map<String, String> stringStringMap) throws PluginParseException {
        // nothing
    }

    @Override
    public Map<String, Object> getContextMap(final Map<String, Object> context) {
        final SlackLink link = (SlackLink) context.get("link");
        ImmutableMap.Builder<String, Object> contextBuilder = notificationConfigurationContextBuilder
                .createGlobalViewContext(link == null ? null : link.getTeamId());
        if (context.containsKey("recentInstall")) {
            contextBuilder.put("recentInstall", context.get("recentInstall"));
        }
        return contextBuilder.build();
    }

}
