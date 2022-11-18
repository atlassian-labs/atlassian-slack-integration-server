package com.atlassian.confluence.plugins.slack.spacetochannel.dataprovider;

import com.atlassian.confluence.plugins.slack.spacetochannel.configuration.SpaceToChannelConfiguration;
import com.atlassian.confluence.plugins.slack.spacetochannel.notifications.SpaceNotificationContext;
import com.atlassian.confluence.plugins.slack.spacetochannel.service.SlackSpaceToChannelService;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.SlackUser;
import com.atlassian.plugins.slack.api.client.SlackClient;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.atlassian.plugins.slack.api.descriptor.NotificationTypeService;
import com.atlassian.plugins.slack.api.notification.NotificationType;
import com.atlassian.plugins.slack.api.routes.SlackRoutesProvider;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.plugins.slack.spi.SlackRoutesProviderFactory;
import com.atlassian.plugins.slack.user.SlackUserManager;
import com.atlassian.plugins.slack.util.ErrorResponse;
import com.atlassian.sal.api.user.UserKey;
import com.atlassian.sal.api.user.UserManager;
import com.github.seratch.jslack.api.model.User;
import io.atlassian.fugue.Either;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SlackGlobalAdminDataProviderTest {
    private static final String TEAM_ID = "T";
    private static final String USER = "USR";
    private static final String SLACK_USER = "SUSR";
    private static final UserKey userKey = new UserKey(USER);

    @Mock
    private SlackSpaceToChannelService spaceToChannelService;
    @Mock
    private UserManager userManager;
    @Mock
    private SlackUserManager slackUserManager;
    @Mock
    private SlackLinkManager slackLinkManager;
    @Mock
    private NotificationTypeService notificationTypeService;
    @Mock
    private SlackClientProvider slackClientProvider;
    @Mock
    private SlackRoutesProviderFactory slackRoutesProviderFactory;
    @Mock
    private SlackLink slackLink;
    @Mock
    private SlackUser slackUser;
    @Mock
    private SlackClient client;
    @Mock
    private User user;
    @Mock
    private SlackRoutesProvider slackRoutesProvider;

    @InjectMocks
    private SlackGlobalAdminDataProvider target;

    @Test
    public void getContextMap_shouldReturnExpectedValues() {
        List<SlackLink> links = Collections.singletonList(slackLink);
        List<SpaceToChannelConfiguration> configs = Collections.emptyList();
        List<NotificationType> notifications = Collections.emptyList();

        when(slackLink.getTeamId()).thenReturn(TEAM_ID);
        when(slackLinkManager.getLinks()).thenReturn(links);
        when(spaceToChannelService.getAllSpaceToChannelConfigurations()).thenReturn(configs);
        when(notificationTypeService.getNotificationTypes(SpaceNotificationContext.KEY)).thenReturn(notifications);
        when(slackRoutesProviderFactory.getProvider(Collections.emptyMap())).thenReturn(slackRoutesProvider);

        Map<String, Object> context = new HashMap<>();
        target.getContextMap(context);

        assertThat(context, hasKey("links"));
        assertThat(((List<?>) context.get("links")), contains(hasProperty("teamId", is(TEAM_ID))));

        assertThat(context, hasEntry(is("configs"), sameInstance(configs)));
        assertThat(context, hasEntry(is("notificationTypes"), sameInstance(notifications)));
        assertThat(context, hasEntry(is("routes"), sameInstance(slackRoutesProvider)));
        assertThat(context, not(hasKey("link")));
    }

    @Test
    public void getContextMap_shouldReturnSlackUserNameWhenSlackLinkAndUserLinkArePresent() {
        when(slackLinkManager.getLinks()).thenReturn(Collections.emptyList());
        when(slackLink.getTeamId()).thenReturn(TEAM_ID);
        when(userManager.getRemoteUserKey()).thenReturn(userKey);
        when(slackUserManager.getByTeamIdAndUserKey(TEAM_ID, USER)).thenReturn(Optional.of(slackUser));
        when(slackClientProvider.withLink(slackLink)).thenReturn(client);
        when(slackUser.getSlackUserId()).thenReturn(SLACK_USER);
        when(client.getUserInfo(SLACK_USER)).thenReturn(Either.right(user));
        when(user.getRealName()).thenReturn("u");

        Map<String, Object> context = new HashMap<>();
        context.put("link", slackLink);

        target.getContextMap(context);

        assertThat(context, hasEntry("slackUserId", SLACK_USER));
        assertThat(context, hasEntry("slackUserName", "u"));
    }

    @Test
    public void getContextMap_shouldNOTReturnSlackUserNameWhenUserLinkIsNotPresent() {
        when(slackLinkManager.getLinks()).thenReturn(Collections.emptyList());
        when(slackLink.getTeamId()).thenReturn(TEAM_ID);
        when(userManager.getRemoteUserKey()).thenReturn(userKey);
        when(slackUserManager.getByTeamIdAndUserKey(TEAM_ID, USER)).thenReturn(Optional.empty());

        Map<String, Object> context = new HashMap<>();
        context.put("link", slackLink);

        target.getContextMap(context);

        assertThat(context, not(hasKey("slackUserName")));
        assertThat(context, not(hasKey("slackUserId")));
    }

    @Test
    public void getContextMap_shouldNOTReturnSlackUserNameWhenSlackUserIsNotFound() {
        when(slackLinkManager.getLinks()).thenReturn(Collections.emptyList());
        when(slackLink.getTeamId()).thenReturn(TEAM_ID);
        when(userManager.getRemoteUserKey()).thenReturn(userKey);
        when(slackUserManager.getByTeamIdAndUserKey(TEAM_ID, USER)).thenReturn(Optional.of(slackUser));
        when(slackClientProvider.withLink(slackLink)).thenReturn(client);
        when(slackUser.getSlackUserId()).thenReturn(SLACK_USER);
        when(client.getUserInfo(SLACK_USER)).thenReturn(Either.left(new ErrorResponse(new Exception(""))));

        Map<String, Object> context = new HashMap<>();
        context.put("link", slackLink);

        target.getContextMap(context);

        assertThat(context, hasEntry("slackUserId", SLACK_USER));
        assertThat(context, hasEntry("slackUserName", SLACK_USER));
    }
}
