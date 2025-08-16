package com.atlassian.jira.plugins.slack.web.actions;

import com.atlassian.jira.help.HelpUrl;
import com.atlassian.jira.help.HelpUrls;
import com.atlassian.jira.junit.rules.AvailableInContainer;
import com.atlassian.jira.junit.rules.MockitoContainer;
import com.atlassian.jira.plugins.slack.manager.ProjectConfigurationManager;
import com.atlassian.jira.plugins.slack.model.dto.ProjectToChannelConfigurationDTO;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.projectconfig.util.ProjectConfigRequestCache;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.velocity.VelocityRequestContextFactory;
import com.atlassian.jira.web.HttpServletVariables;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.SlackUser;
import com.atlassian.plugins.slack.api.client.SlackClient;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.atlassian.plugins.slack.api.routes.SlackRoutesProvider;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.plugins.slack.spi.SlackRoutesProviderFactory;
import com.atlassian.plugins.slack.user.SlackUserManager;
import com.atlassian.webresource.api.assembler.PageBuilderService;
import com.github.seratch.jslack.api.model.User;
import io.atlassian.fugue.Either;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.atlassian.jira.plugins.slack.web.actions.SlackConfigureProjectAction.ADVANCED_SEARCH_HELP_KEY;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class SlackConfigureProjectActionTest {
    @Mock
    private VelocityRequestContextFactory contextFactory;
    @Mock
    private PageBuilderService pageBuilderService;
    @Mock
    private ProjectConfigRequestCache projectConfigRequestCache;
    @Mock
    private ProjectConfigurationManager projectConfigurationManager;
    @Mock
    private SlackUserManager slackUserManager;
    @Mock
    private SlackRoutesProviderFactory slackRoutesProviderFactory;
    @Mock
    private HelpUrls helpUrls;
    @Mock
    private SlackClientProvider slackClientProvider;
    @Mock
    private SlackLinkManager slackLinkManager;

    @AvailableInContainer
    @Mock
    private ProjectManager projectManager;

    @AvailableInContainer
    @Mock
    private HttpServletVariables httpServletVariables;

    @AvailableInContainer
    @Mock
    private JiraAuthenticationContext jiraAuthenticationContext;

    @Mock
    private ApplicationUser applicationUser;
    @Mock
    private Project project;
    @Mock
    private SlackLink link;
    @Mock
    private SlackUser slackUser;
    @Mock
    private SlackClient client;
    @Mock
    private ProjectToChannelConfigurationDTO config;
    @Mock
    private HelpUrl helpUrl;
    @Mock
    private HttpServletRequest request;
    @Mock
    private SlackRoutesProvider slackRoutesProvider;
    @Mock
    private User user;

    @Rule
    public MockitoContainer mockitoContainer = new MockitoContainer(this);

    @InjectMocks
    private SlackConfigureProjectAction target;

    @Test
    public void getDataMap() {
        when(applicationUser.getDisplayName()).thenReturn("");
        target.setProjectKey("P");
        when(projectManager.getProjectObjByKey("P")).thenReturn(project);
        when(project.getId()).thenReturn(7L);
        when(project.getName()).thenReturn("Project");
        when(projectConfigurationManager.getConfiguration(7L)).thenReturn(config);
        when(helpUrls.getUrl(ADVANCED_SEARCH_HELP_KEY)).thenReturn(helpUrl);
        when(httpServletVariables.getHttpRequest()).thenReturn(request);
        when(request.getParameter("teamId")).thenReturn("T");
        when(slackLinkManager.getLinkByTeamId("T")).thenReturn(Either.right(link));
        List<SlackLink> links = new ArrayList<>();
        when(slackLinkManager.getLinks()).thenReturn(links);
        when(slackRoutesProviderFactory.getProvider(any())).thenReturn(slackRoutesProvider);
        when(jiraAuthenticationContext.getLoggedInUser()).thenReturn(applicationUser);
        when(jiraAuthenticationContext.getUser()).thenReturn(applicationUser);
        when(applicationUser.getKey()).thenReturn("U");
        when(link.getTeamId()).thenReturn("T");
        when(slackUserManager.getByTeamIdAndUserKey("T", "U")).thenReturn(Optional.of(slackUser));
        when(slackUser.getSlackUserId()).thenReturn("slackUserId");
        when(slackClientProvider.withLink(link)).thenReturn(client);
        when(client.getUserInfo("slackUserId")).thenReturn(Either.right(user));
        when(user.getRealName()).thenReturn("slackUserName");

        Map<String, Object> result = target.getDataMap();

        assertThat(result, hasEntry(is("links"), sameInstance(links)));
        assertThat(result, hasEntry("projectKey", "P"));
        assertThat(result, hasEntry(is("projectConfiguration"), sameInstance(config)));
        assertThat(result, hasEntry("projectId", 7L));
        assertThat(result, hasEntry(is("routes"), sameInstance(slackRoutesProvider)));
        assertThat(result, hasEntry("projectName", "Project"));
        assertThat(result, hasEntry("slackUserName", "slackUserName"));
        assertThat(result, hasEntry("slackUserId", "slackUserId"));
        assertThat(result, hasEntry(is("jqlHelpUrl"), sameInstance(helpUrl)));
        assertThat(result, hasEntry(is("link"), sameInstance(link)));
    }
}
