package com.atlassian.bitbucket.plugins.slack.notification.configuration;

import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.server.ApplicationPropertiesService;
import com.atlassian.bitbucket.util.Page;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.SlackUser;
import com.atlassian.plugins.slack.api.client.SlackClient;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.atlassian.plugins.slack.api.routes.SlackRoutesProvider;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.plugins.slack.spi.SlackRoutesProviderFactory;
import com.atlassian.plugins.slack.user.SlackUserManager;
import com.atlassian.sal.api.user.UserKey;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import com.github.seratch.jslack.api.model.User;
import com.google.common.collect.ImmutableMap;
import io.atlassian.fugue.Either;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class NotificationConfigurationContextBuilderTest {
    @Mock
    SlackLinkManager slackLinkManager;
    @Mock
    SlackUserManager slackUserManager;
    @Mock
    SlackClientProvider slackClientProvider;
    @Mock
    NotificationConfigurationService notificationConfigService;
    @Mock
    ApplicationPropertiesService propertiesService;
    @Mock
    UserManager userManager;
    @Mock
    SlackRoutesProviderFactory slackRoutesProviderFactory;

    @Mock
    Repository repository;
    @Mock
    SlackLink slackLink;
    @Mock
    UserProfile userProfile;
    @Mock
    SlackClient slackClient;
    @Mock
    SlackUser slackUser;
    @Mock
    User user;
    @Mock
    SlackRoutesProvider slackRoutesProvider;
    @Mock
    Page<RepositoryConfiguration> configsPage;

    @InjectMocks
    NotificationConfigurationContextBuilder target;

    @Test
    void addSlackViewContext() {
        List<SlackLink> links = new ArrayList<>();
        String teamId = "someTeamId";
        String userKey = "someUserKey";
        String slackUserId = "someSlackUserId";
        String slackUserName = "someSlackUserRealName";
        when(slackLinkManager.getLinks()).thenReturn(links);
        when(slackLinkManager.getLinkByTeamId(teamId)).thenReturn(Either.right(slackLink));
        when(slackLink.getTeamId()).thenReturn(teamId);
        when(userManager.getRemoteUser()).thenReturn(userProfile);
        when(userProfile.getUserKey()).thenReturn(new UserKey(userKey));
        when(slackClientProvider.withLink(slackLink)).thenReturn(slackClient);
        when(slackUserManager.getByTeamIdAndUserKey(teamId, userKey)).thenReturn(Optional.of(slackUser));
        when(slackUser.getSlackUserId()).thenReturn(slackUserId);
        when(slackClient.getUserInfo(slackUserId)).thenReturn(Either.right(user));
        when(user.getRealName()).thenReturn(slackUserName);
        when(slackRoutesProviderFactory.getProvider(argThat(map -> map.get("repository") == repository)))
                .thenReturn(slackRoutesProvider);

        Map<String, Object> context = target.addSlackViewContext(repository, teamId, ImmutableMap.builder()).build();

        assertThat(context, Matchers.hasEntry("links", links));
        assertThat(context, Matchers.hasEntry("slackUserId", slackUserId));
        assertThat(context, Matchers.hasEntry("slackUserName", slackUserName));
        assertThat(context, Matchers.hasEntry("routes", slackRoutesProvider));
    }

    @Test
    void createGlobalViewContext() {
        String teamId = "someTeamId";
        when(notificationConfigService.search(any(), any())).thenReturn(configsPage);
        when(slackLinkManager.getLinkByTeamId(teamId)).thenReturn(Either.left(new Exception()));
        when(slackRoutesProviderFactory.getProvider(any())).thenReturn(slackRoutesProvider);

        Map<String, Object> context = target.createGlobalViewContext(teamId).build();

        assertThat(context, Matchers.hasEntry("configsPage", configsPage));
    }

    @Test
    void createRepositoryViewContext() {
        String teamId = "someTeamId";
        when(notificationConfigService.search(any(), any())).thenReturn(configsPage);
        when(slackLinkManager.getLinkByTeamId(teamId)).thenReturn(Either.left(new Exception()));
        when(slackRoutesProviderFactory.getProvider(argThat(map -> map.get("repository") == repository)))
                .thenReturn(slackRoutesProvider);

        Map<String, Object> context = target.createRepositoryViewContext(teamId, repository).build();

        assertThat(context, Matchers.hasEntry("configsPage", configsPage));
        assertThat(context, Matchers.hasEntry("repository", repository));
    }
}
