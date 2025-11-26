package com.atlassian.jira.plugins.slack.spi.impl;

import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.UrlMode;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.net.URI;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

public class ProjectAwareSlackRoutesProviderTest {
    private static final String PROJECT_KEY = "PK";

    @Mock
    private ApplicationProperties applicationProperties;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private ProjectAwareSlackRoutesProvider target;

    @Before
    public void beforeEach() {
        when(applicationProperties.getBaseUrl(UrlMode.ABSOLUTE)).thenReturn("https://localhost:2990/jira");
        target = new ProjectAwareSlackRoutesProvider(PROJECT_KEY, applicationProperties);
    }

    @Test
    public void getAdminConfigurationPage() {
        URI adminConfigurationPage = target.getAdminConfigurationPage();

        assertThat(adminConfigurationPage, equalTo(URI.create("https://localhost:2990/jira/secure/ConfigureSlack.jspa?projectKey=PK")));
    }
    
    @Test
    public void getGlobalConfigurationPage() {
        URI globalConfigurationPage = target.getGlobalConfigurationPage();

        assertThat(globalConfigurationPage, equalTo(URI.create("https://localhost:2990/jira/plugins/servlet/slack/configure")));
    }
}
