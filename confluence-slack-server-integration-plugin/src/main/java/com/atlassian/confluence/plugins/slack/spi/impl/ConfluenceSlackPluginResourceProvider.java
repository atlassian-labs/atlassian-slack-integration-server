package com.atlassian.confluence.plugins.slack.spi.impl;

import com.atlassian.confluence.plugins.slack.spacetochannel.notifications.ConfluencePersonalNotificationTypes;
import com.atlassian.plugin.webresource.UrlMode;
import com.atlassian.plugin.webresource.WebResourceUrlProvider;
import com.atlassian.plugins.slack.spi.SlackPluginResourceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class ConfluenceSlackPluginResourceProvider implements SlackPluginResourceProvider {
    private final WebResourceUrlProvider urlProvider;

    @Autowired
    public ConfluenceSlackPluginResourceProvider(final WebResourceUrlProvider urlProvider) {
        this.urlProvider = urlProvider;
    }

    public String getInstallingImage() {
        return getImageUrl("images/slack-needs-signin.svg");
    }

    public String getUninstallingImage() {
        return getImageUrl("images/slack-needs-signin.svg");
    }

    public String getInstalledImage() {
        return getImageUrl("images/big-news.svg");
    }

    private String getImageUrl(final String imageName) {
        return urlProvider.getStaticPluginResourceUrl(
                getPluginKey() + ":" + "slack-image-resources",
                imageName,
                UrlMode.ABSOLUTE);
    }

    @Override
    public String getPluginKey() {
        return "com.atlassian.confluence.plugins.confluence-slack-server-integration-plugin";
    }

    @Override
    public List<Enum<?>> getPersonalConfigurationKeys() {
        return Arrays.asList(ConfluencePersonalNotificationTypes.values());
    }
}
