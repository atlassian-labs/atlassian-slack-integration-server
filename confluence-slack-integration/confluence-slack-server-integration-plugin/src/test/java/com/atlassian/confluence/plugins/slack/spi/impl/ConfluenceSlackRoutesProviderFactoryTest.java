package com.atlassian.confluence.plugins.slack.spi.impl;

import com.atlassian.confluence.spaces.Space;
import com.atlassian.plugins.slack.api.routes.SlackRoutesProvider;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.UrlMode;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ConfluenceSlackRoutesProviderFactoryTest {
    private static final String SPACE_KEY = "SK";
    private static final String BASE_URL = "url";

    @Mock
    private ApplicationProperties applicationProperties;
    @Mock
    private SlackRoutesProvider defaultSlackRoutesProvider;
    @Mock
    private Space space;

    @InjectMocks
    private ConfluenceSlackRoutesProviderFactory target;

    @Test
    public void method_shouldReturnDefaultProviderWhenNoSpaceIsProvided() {
        SlackRoutesProvider result = target.getProvider(Collections.emptyMap());
        assertThat(result, sameInstance(defaultSlackRoutesProvider));
    }

    @Test
    public void method_shouldReturnSpaceAwareProviderWhenSpaceKeyIsProvided() {
        when(applicationProperties.getBaseUrl(UrlMode.ABSOLUTE)).thenReturn(BASE_URL);
        SlackRoutesProvider result = target.getProvider(ImmutableMap.of("spaceKey", SPACE_KEY));
        assertThat(result, instanceOf(SpaceAwareSlackRoutesProvider.class));
        assertThat(result.getAdminConfigurationPage().getQuery(), containsString("key=" + SPACE_KEY));
    }

    @Test
    public void method_shouldReturnSpaceAwareProviderWhenSpaceEntityIsProvided() {
        when(applicationProperties.getBaseUrl(UrlMode.ABSOLUTE)).thenReturn(BASE_URL);
        when(space.getKey()).thenReturn(SPACE_KEY);
        SlackRoutesProvider result = target.getProvider(ImmutableMap.of("space", space));
        assertThat(result, instanceOf(SpaceAwareSlackRoutesProvider.class));
        assertThat(result.getAdminConfigurationPage().getQuery(), containsString("key=" + SPACE_KEY));
    }
}
