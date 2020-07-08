package com.atlassian.jira.plugins.slack.spi.impl;

import com.atlassian.jira.project.Project;
import com.atlassian.plugins.slack.api.routes.SlackRoutesProvider;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.UrlMode;
import com.google.common.collect.ImmutableMap;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.when;

public class JiraSlackRoutesProviderFactoryTest {
    private static final String PROJECT_KEY = "PK";
    private static final String BASE_URL = "url";

    @Mock
    private ApplicationProperties applicationProperties;
    @Mock
    private SlackRoutesProvider defaultSlackRoutesProvider;
    @Mock
    private Project project;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private JiraSlackRoutesProviderFactory target;

    @Test
    public void method_shouldReturnDefaultProviderWhenNoProjectIsProvided() {
        SlackRoutesProvider result = target.getProvider(Collections.emptyMap());
        assertThat(result, sameInstance(defaultSlackRoutesProvider));
    }

    @Test
    public void method_shouldReturnSpaceAwareProviderWhenProjectKeyIsProvided() {
        when(applicationProperties.getBaseUrl(UrlMode.ABSOLUTE)).thenReturn(BASE_URL);
        SlackRoutesProvider result = target.getProvider(ImmutableMap.of("projectKey", PROJECT_KEY));
        assertThat(result, instanceOf(ProjectAwareSlackRoutesProvider.class));
        assertThat(result.getAdminConfigurationPage().getQuery(), containsString("projectKey=" + PROJECT_KEY));
    }
}
