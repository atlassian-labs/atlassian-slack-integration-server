package com.atlassian.confluence.plugins.slack.spacetochannel.actions;

import com.atlassian.confluence.plugins.slack.spacetochannel.configuration.SpaceToChannelConfiguration;
import com.atlassian.confluence.plugins.slack.spacetochannel.notifications.SpaceNotificationContext;
import com.atlassian.confluence.plugins.slack.spacetochannel.service.SlackSpaceToChannelService;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.spaces.SpaceManager;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugins.slack.analytics.AnalyticsContextProvider;
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
import com.atlassian.sal.api.user.UserKey;
import com.github.seratch.jslack.api.model.User;
import io.atlassian.fugue.Either;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SlackViewSpaceConfigurationActionTest {
    private static final String TEAM_ID = "someTeamId";
    private static final String TEAM_ID2 = "someTeamId2";
    private static final String SPACE_KEY = "SPACE";
    private static final String SLACK_USER_ID = "someSlackUser";
    private static final String SLACK_USER_NAME = "someSlackUserName";
    private static final String USER = "USR";
    private static final UserKey userKey = new UserKey(USER);

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private SlackSpaceToChannelService slackSpaceToChannelService;
    @Mock
    private SlackLinkManager slackLinkManager;
    @Mock
    private SlackUserManager slackUserManager;
    @Mock
    private SlackRoutesProviderFactory slackRoutesProviderFactory;
    @Mock
    private NotificationTypeService notificationTypeService;
    @Mock
    private SlackClientProvider slackClientProvider;
    @Mock
    private SpaceManager spaceManager;
    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private AnalyticsContextProvider analyticsContextProvider;

    @Mock
    private SlackLink link;
    @Mock
    private SlackLink link2;
    @Mock
    private SlackUser slackUser;
    @Mock
    private Space space;
    @Mock
    private ConfluenceUser confluenceUser;
    @Mock
    private User user;
    @Mock
    private SlackClient slackClient;
    @Mock
    private List<NotificationType> notificationTypes;
    @Mock
    private SpaceToChannelConfiguration spaceToChannelConfiguration;
    @Mock
    private SlackRoutesProvider slackRoutesProvider;

    @Captor
    private ArgumentCaptor<Map<String, Object>> mapArgumentCaptor;

    private SlackViewSpaceConfigurationActionMock servlet;

    class SlackViewSpaceConfigurationActionMock extends SlackViewSpaceConfigurationAction {

        public SlackViewSpaceConfigurationActionMock(SlackSpaceToChannelService slackSpaceToChannelService,
                                                     SlackLinkManager slackLinkManager,
                                                     SlackUserManager slackUserManager,
                                                     SlackRoutesProviderFactory slackRoutesProviderFactory,
                                                     NotificationTypeService notificationTypeService,
                                                     SlackClientProvider slackClientProvider,
                                                     EventPublisher eventPublisher,
                                                     AnalyticsContextProvider analyticsContextProvider) {
            super(slackSpaceToChannelService, slackLinkManager, slackUserManager, slackRoutesProviderFactory,
                    notificationTypeService, slackClientProvider, eventPublisher, analyticsContextProvider);
        }

        @Override
        protected HttpServletRequest getActiveRequest() {
            return SlackViewSpaceConfigurationActionTest.this.request;
        }
    }

    @BeforeEach
    public void setUp() {
        servlet = new SlackViewSpaceConfigurationActionMock(slackSpaceToChannelService, slackLinkManager,
                slackUserManager, slackRoutesProviderFactory, notificationTypeService, slackClientProvider,
                eventPublisher, analyticsContextProvider);

        servlet.setKey(SPACE_KEY);
        servlet.setSpaceManager(spaceManager);
        AuthenticatedUserThreadLocal.set(confluenceUser);
    }

    @AfterEach
    public void tearDown() {
        AuthenticatedUserThreadLocal.reset();
    }

    @Test
    public void execute_shouldReturnInstallWhenLinksIsEmpty() {
        when(slackLinkManager.getLinks()).thenReturn(Collections.emptyList());
        String result = servlet.execute();

        assertThat(result, is("install"));
    }

    @Test
    public void execute_shouldReturnSuccessWhenThereAreLinksAndUserNameWhenConnected() {
        when(slackLinkManager.getLinks()).thenReturn(Arrays.asList(link, link2));
        when(link.getTeamId()).thenReturn(TEAM_ID);
        when(link2.getTeamId()).thenReturn(TEAM_ID2);
        when(spaceManager.getSpace(SPACE_KEY)).thenReturn(space);

        when(confluenceUser.getKey()).thenReturn(userKey);
        when(slackUserManager.getByTeamIdAndUserKey(TEAM_ID2, USER)).thenReturn(Optional.of(slackUser));
        when(slackClientProvider.withLink(link2)).thenReturn(slackClient);
        when(slackUser.getSlackUserId()).thenReturn(SLACK_USER_ID);
        when(slackClient.getUserInfo(SLACK_USER_ID)).thenReturn(Either.right(user));
        when(user.getRealName()).thenReturn(SLACK_USER_NAME);
        when(slackSpaceToChannelService.getSpaceToChannelConfiguration(SPACE_KEY)).thenReturn(spaceToChannelConfiguration);
        when(slackRoutesProviderFactory.getProvider(mapArgumentCaptor.capture())).thenReturn(slackRoutesProvider);

        servlet.setTeamId(TEAM_ID2);

        String result = servlet.execute();

        assertThat(result, is("success"));
        assertThat(servlet.getSlackUserName(), is(SLACK_USER_NAME));
        assertThat(servlet.getLink(), is(link2));
        assertThat(servlet.getLinks(), containsInAnyOrder(link, link2));
        assertThat(servlet.getConfigs(), containsInAnyOrder(spaceToChannelConfiguration));
        assertThat(servlet.getRoutes(), sameInstance(slackRoutesProvider));
        assertThat(mapArgumentCaptor.getValue(), hasEntry("space", space));
    }

    @Test
    public void execute_shouldReturnSuccessWhenThereAreLinksAndNullUserNameWhenNotConnected() {
        when(slackLinkManager.getLinks()).thenReturn(Collections.singletonList(link));
        when(link.getTeamId()).thenReturn(TEAM_ID);
        when(spaceManager.getSpace(SPACE_KEY)).thenReturn(space);

        when(confluenceUser.getKey()).thenReturn(userKey);
        when(slackUserManager.getByTeamIdAndUserKey(TEAM_ID, USER)).thenReturn(Optional.empty());

        String result = servlet.execute();

        assertThat(result, is("success"));
        assertThat(servlet.getSlackUserName(), nullValue());
    }

    @Test
    public void getNotificationTypes_shouldReturnExpectedValue() {
        when(notificationTypeService.getNotificationTypes(SpaceNotificationContext.KEY))
                .thenReturn(notificationTypes);

        List<NotificationType> result = servlet.getNotificationTypes();

        assertThat(result, sameInstance(notificationTypes));
    }
}
