package com.atlassian.confluence.plugins.slack.spi.impl;

import com.atlassian.plugin.webresource.UrlMode;
import com.atlassian.plugin.webresource.WebResourceUrlProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ConfluenceSlackPluginResourceProviderTest {
    private static final String URL = "url";

    @Mock
    private WebResourceUrlProvider webResourceUrlProvider;

    @InjectMocks
    private ConfluenceSlackPluginResourceProvider target;

    @Test
    public void getInstallingImage_shouldReturnExpectedValue() {
        when(webResourceUrlProvider.getStaticPluginResourceUrl(
                "com.atlassian.confluence.plugins.confluence-slack-server-integration-plugin:slack-image-resources",
                "images/slack-needs-signin.svg",
                UrlMode.ABSOLUTE)
        ).thenReturn(URL);

        String result = target.getInstallingImage();

        assertThat(result, is(URL));
    }

    @Test
    public void getUninstallingImage_shouldReturnExpectedValue() {
        when(webResourceUrlProvider.getStaticPluginResourceUrl(
                "com.atlassian.confluence.plugins.confluence-slack-server-integration-plugin:slack-image-resources",
                "images/slack-needs-signin.svg",
                UrlMode.ABSOLUTE)
        ).thenReturn(URL);

        String result = target.getUninstallingImage();

        assertThat(result, is(URL));
    }

    @Test
    public void getInstalledImage_shouldReturnExpectedValue() {
        when(webResourceUrlProvider.getStaticPluginResourceUrl(
                "com.atlassian.confluence.plugins.confluence-slack-server-integration-plugin:slack-image-resources",
                "images/big-news.svg",
                UrlMode.ABSOLUTE)
        ).thenReturn(URL);

        String result = target.getInstalledImage();

        assertThat(result, is(URL));
    }
}
