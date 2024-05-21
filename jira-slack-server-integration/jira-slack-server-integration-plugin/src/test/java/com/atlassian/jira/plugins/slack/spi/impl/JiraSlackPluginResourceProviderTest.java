package com.atlassian.jira.plugins.slack.spi.impl;

import com.atlassian.webresource.api.UrlMode;
import com.atlassian.webresource.api.WebResourceUrlProvider;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

public class JiraSlackPluginResourceProviderTest {
    private static final String URL = "url";

    @Mock
    private WebResourceUrlProvider webResourceUrlProvider;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private JiraSlackPluginResourceProvider target;

    @Test
    public void getInstallingImage_shouldReturnExpectedValue() {
        when(webResourceUrlProvider.getStaticPluginResourceUrl(
                "com.atlassian.jira.plugins.jira-slack-server-integration-plugin:slack-jira-image-resources",
                "images/slack-needs-signin.svg",
                UrlMode.ABSOLUTE)
        ).thenReturn(URL);

        String result = target.getInstallingImage();

        assertThat(result, is(URL));
    }

    @Test
    public void getUninstallingImage_shouldReturnExpectedValue() {
        when(webResourceUrlProvider.getStaticPluginResourceUrl(
                "com.atlassian.jira.plugins.jira-slack-server-integration-plugin:slack-jira-image-resources",
                "images/slack-needs-signin.svg",
                UrlMode.ABSOLUTE)
        ).thenReturn(URL);

        String result = target.getUninstallingImage();

        assertThat(result, is(URL));
    }

    @Test
    public void getInstalledImage_shouldReturnExpectedValue() {
        when(webResourceUrlProvider.getStaticPluginResourceUrl(
                "com.atlassian.jira.plugins.jira-slack-server-integration-plugin:slack-jira-image-resources",
                "images/big-news.svg",
                UrlMode.ABSOLUTE)
        ).thenReturn(URL);

        String result = target.getInstalledImage();

        assertThat(result, is(URL));
    }
}
