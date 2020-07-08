package com.atlassian.jira.plugins.slack.web.contextproviders;

import com.atlassian.jira.help.HelpUrls;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.OrderableField;
import com.atlassian.jira.plugins.slack.manager.ProjectConfigurationManager;
import com.atlassian.jira.plugins.slack.model.dto.ProjectToChannelConfigurationDTO;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.UserProjectHistoryManager;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.SlackUser;
import com.atlassian.plugins.slack.api.client.SlackClient;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.atlassian.plugins.slack.api.routes.SlackRoutesProvider;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.plugins.slack.spi.SlackRoutesProviderFactory;
import com.atlassian.plugins.slack.user.SlackUserManager;
import com.atlassian.plugins.slack.util.ErrorResponse;
import com.github.seratch.jslack.api.model.User;
import io.atlassian.fugue.Either;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

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

@SuppressWarnings("deprecation")
public class SlackGlobalAdminDataProviderTest {
    private static final String USER = "USR";
    private static final String SLACK_USER = "SUSR";

    @Mock
    private ProjectConfigurationManager projectConfigurationManager;
    @Mock
    private FieldManager fieldManager;
    @Mock
    private UserProjectHistoryManager userProjectHistoryManagerBridge;
    @Mock
    private JiraAuthenticationContext jiraAuthenticationContext;
    @Mock
    private SlackUserManager slackUserManager;
    @Mock
    private SlackClientProvider slackClientProvider;
    @Mock
    private HelpUrls helpUrls;
    @Mock
    private SlackLinkManager slackLinkManager;
    @Mock
    private SlackRoutesProviderFactory slackRoutesProviderFactory;

    @Mock
    private ApplicationUser applicationUser;
    @Mock
    private ProjectToChannelConfigurationDTO projectToChannelConfigurationDTO;
    @Mock
    private SlackLink link;
    @Mock
    private SlackClient client;
    @Mock
    private SlackRoutesProvider slackRoutesProvider;
    @Mock
    private OrderableField orderableField;
    @Mock
    private Project project;
    @Mock
    private SlackUser slackUser;
    @Mock
    private User user;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private SlackGlobalAdminDataProvider target;

    @Test
    public void getContextMap_shouldReturnExpectedValues() {
        List<SlackLink> links = Collections.singletonList(link);
        List<ProjectToChannelConfigurationDTO> configs = Collections.singletonList(projectToChannelConfigurationDTO);

        when(link.getTeamId()).thenReturn("T");
        when(slackLinkManager.getLinks()).thenReturn(links);
        when(projectConfigurationManager.getConfigurations(-1, -1)).thenReturn(configs);
        when(slackRoutesProviderFactory.getProvider(Collections.emptyMap())).thenReturn(slackRoutesProvider);
        when(jiraAuthenticationContext.getLoggedInUser()).thenReturn(applicationUser);
        when(fieldManager.getField(IssueFieldConstants.PROJECT)).thenReturn(orderableField);
        when(userProjectHistoryManagerBridge.getCurrentProject(Permissions.BROWSE, applicationUser)).thenReturn(project);

        Map<String, Object> context = new HashMap<>();
        target.getContextMap(context);

        assertThat(context, hasKey("links"));
        assertThat(((List<?>) context.get("links")), contains(hasProperty("teamId", is("T"))));

        assertThat(context, hasEntry(is("projectConfigurations"), sameInstance(configs)));
        assertThat(context, hasEntry(is("routes"), sameInstance(slackRoutesProvider)));
        assertThat(context, not(hasKey("link")));
    }

    @Test
    public void getContextMap_shouldReturnSlackUserNameWhenSlackLinkAndUserLinkArePresent() {
        when(slackLinkManager.getLinks()).thenReturn(Collections.emptyList());
        when(link.getTeamId()).thenReturn("T");
        when(jiraAuthenticationContext.getLoggedInUser()).thenReturn(applicationUser);
        when(fieldManager.getField(IssueFieldConstants.PROJECT)).thenReturn(orderableField);
        when(userProjectHistoryManagerBridge.getCurrentProject(Permissions.BROWSE, applicationUser)).thenReturn(project);
        when(applicationUser.getKey()).thenReturn(USER);
        when(slackUserManager.getByTeamIdAndUserKey("T", USER)).thenReturn(Optional.of(slackUser));
        when(slackClientProvider.withLink(link)).thenReturn(client);
        when(slackUser.getSlackUserId()).thenReturn(SLACK_USER);
        when(client.getUserInfo(SLACK_USER)).thenReturn(Either.right(user));
        when(user.getRealName()).thenReturn("u");

        Map<String, Object> context = new HashMap<>();
        context.put("link", link);

        target.getContextMap(context);

        assertThat(context, hasEntry("slackUserId", SLACK_USER));
        assertThat(context, hasEntry("slackUserName", "u"));
    }

    @Test
    public void getContextMap_shouldNOTReturnSlackUserNameWhenUserLinkIsNotPresent() {
        when(slackLinkManager.getLinks()).thenReturn(Collections.emptyList());
        when(link.getTeamId()).thenReturn("T");
        when(jiraAuthenticationContext.getLoggedInUser()).thenReturn(applicationUser);
        when(fieldManager.getField(IssueFieldConstants.PROJECT)).thenReturn(orderableField);
        when(userProjectHistoryManagerBridge.getCurrentProject(Permissions.BROWSE, applicationUser)).thenReturn(project);
        when(applicationUser.getKey()).thenReturn(USER);
        when(slackUserManager.getByTeamIdAndUserKey("T", USER)).thenReturn(Optional.empty());

        Map<String, Object> context = new HashMap<>();
        context.put("link", link);

        target.getContextMap(context);

        assertThat(context, not(hasKey("slackUserName")));
        assertThat(context, not(hasKey("slackUserId")));
    }

    @Test
    public void getContextMap_shouldNOTReturnSlackUserNameWhenSlackUserIsNotFound() {
        when(slackLinkManager.getLinks()).thenReturn(Collections.emptyList());
        when(link.getTeamId()).thenReturn("T");
        when(jiraAuthenticationContext.getLoggedInUser()).thenReturn(applicationUser);
        when(fieldManager.getField(IssueFieldConstants.PROJECT)).thenReturn(orderableField);
        when(userProjectHistoryManagerBridge.getCurrentProject(Permissions.BROWSE, applicationUser)).thenReturn(project);
        when(applicationUser.getKey()).thenReturn(USER);
        when(slackUserManager.getByTeamIdAndUserKey("T", USER)).thenReturn(Optional.of(slackUser));
        when(slackClientProvider.withLink(link)).thenReturn(client);
        when(slackUser.getSlackUserId()).thenReturn(SLACK_USER);
        when(client.getUserInfo(SLACK_USER)).thenReturn(Either.left(new ErrorResponse(new Exception(""))));

        Map<String, Object> context = new HashMap<>();
        context.put("link", link);

        target.getContextMap(context);

        assertThat(context, hasEntry("slackUserId", SLACK_USER));
        assertThat(context, hasEntry("slackUserName", SLACK_USER));
    }
}
