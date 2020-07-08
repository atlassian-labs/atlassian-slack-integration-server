package com.atlassian.plugins.slack.admin;

import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.routes.SlackRoutesProvider;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.plugins.slack.spi.SlackPluginResourceProvider;
import com.atlassian.plugins.slack.spi.SlackRoutesProviderFactory;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.UrlMode;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.net.URI;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class SlackConfigurationScreenContextProviderTest {
    @Mock
    private SlackPluginResourceProvider slackPluginResourceProvider;
    @Mock
    private SlackRoutesProviderFactory slackRoutesProviderFactory;
    @Mock
    private SlackRoutesProvider slackRoutesProvider;
    @Mock
    private ApplicationProperties applicationProperties;
    @Mock
    private SlackLinkManager slackLinkManager;
    @Mock
    private SlackLink slackLink;
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();
    @InjectMocks
    private SlackConfigurationScreenContextProvider provider;

    @Test
    public void getContextMap_shouldBuildExpectedContext() {
        Map<String, Object> presetContext = emptyMap();
        String installedImage = "someInstalledImage";
        String uninstallingImage = "someUninstallingImage";
        String configurationPage = "someConfigurationPage";
        String invitePage = "someInvitePage";
        String baseUrl = "baseUrl";
        when(slackPluginResourceProvider.getInstalledImage()).thenReturn(installedImage);
        when(slackPluginResourceProvider.getUninstallingImage()).thenReturn(uninstallingImage);
        when(slackRoutesProviderFactory.getProvider(presetContext)).thenReturn(slackRoutesProvider);
        when(slackRoutesProvider.getAdminConfigurationPage()).thenReturn(URI.create(configurationPage));
        when(slackRoutesProvider.baseUrl()).thenReturn(URI.create(baseUrl));
        when(applicationProperties.getBaseUrl(any(UrlMode.class))).thenReturn(baseUrl);
        when(slackLinkManager.getLinks()).thenReturn(singletonList(slackLink));

        Map<String, Object> context = provider.getContextMap(presetContext);

        assertThat(context, hasEntry("installedImage", installedImage));
        assertThat(context, hasEntry("uninstallingImage", uninstallingImage));
        assertThat(context, hasEntry("baseUrl", baseUrl));
        assertThat(context, hasKey("routes"));
        Map<String, Object> routes = (Map<String, Object>) context.get("routes");
        assertThat(routes, hasEntry("adminConfigurationPage", configurationPage));
        assertThat(routes, hasEntry("baseUrl", baseUrl));
    }
}
