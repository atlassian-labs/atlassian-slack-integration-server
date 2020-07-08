package com.atlassian.bitbucket.plugins.slack.spi.impl;

import com.atlassian.bitbucket.plugins.slack.notification.BitbucketPersonalNotificationTypes;
import com.atlassian.plugin.webresource.UrlMode;
import com.atlassian.plugin.webresource.WebResourceUrlProvider;
import com.atlassian.plugins.slack.spi.SlackPluginResourceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class BitbucketSlackPluginResourceProvider implements SlackPluginResourceProvider {
    private final WebResourceUrlProvider urlProvider;

    @Autowired
    public BitbucketSlackPluginResourceProvider(final WebResourceUrlProvider urlProvider) {
        this.urlProvider = urlProvider;
    }

    public String getInstallingImage() {
        return getImageUrl("images/slack-installing.svg");
    }

    public String getUninstallingImage() {
        return getImageUrl("images/slack-installing.svg");
    }

    public String getInstalledImage() {
        return getImageUrl("images/slack-installed.svg");
    }

    private String getImageUrl(final String imageName) {
        return urlProvider.getStaticPluginResourceUrl(
                getPluginKey() + ":" + "slack-image-resources",
                imageName,
                UrlMode.ABSOLUTE);
    }

    @Override
    public String getPluginKey() {
        return "com.atlassian.bitbucket.plugins.bitbucket-slack-server-integration-plugin";
    }

    @Override
    public List<Enum<?>> getPersonalConfigurationKeys() {
        return Arrays.asList(BitbucketPersonalNotificationTypes.values());
    }
}
