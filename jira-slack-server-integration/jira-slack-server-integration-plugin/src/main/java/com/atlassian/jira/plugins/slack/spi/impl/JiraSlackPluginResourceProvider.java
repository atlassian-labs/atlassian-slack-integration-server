package com.atlassian.jira.plugins.slack.spi.impl;

import com.atlassian.jira.plugins.slack.model.JiraPersonalNotificationTypes;
import com.atlassian.jira.plugins.slack.util.PluginConstants;
import com.atlassian.plugin.webresource.UrlMode;
import com.atlassian.plugin.webresource.WebResourceUrlProvider;
import com.atlassian.plugins.slack.spi.SlackPluginResourceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component("slackPluginResourceProvider")
public class JiraSlackPluginResourceProvider implements SlackPluginResourceProvider {
    private final WebResourceUrlProvider urlProvider;

    @Autowired
    public JiraSlackPluginResourceProvider(final WebResourceUrlProvider urlProvider) {
        this.urlProvider = urlProvider;
    }

    @Override
    public String getInstallingImage() {
        return getImageUrl("images/slack-needs-signin.svg");
    }

    @Override
    public String getUninstallingImage() {
        return getImageUrl("images/slack-needs-signin.svg");
    }

    @Override
    public String getInstalledImage() {
        return getImageUrl("images/big-news.svg");
    }

    @Override
    public String getPluginKey() {
        return PluginConstants.PLUGIN_KEY;
    }

    private String getImageUrl(String imageName) {
        return urlProvider.getStaticPluginResourceUrl(
                getPluginKey() + ":" + PluginConstants.SLACK_JIRA_GLOBAL_IMAGE_RESOURCES,
                imageName,
                UrlMode.ABSOLUTE);
    }

    @Override
    public List<Enum<?>> getPersonalConfigurationKeys() {
        return Arrays.asList(JiraPersonalNotificationTypes.values());
    }
}
