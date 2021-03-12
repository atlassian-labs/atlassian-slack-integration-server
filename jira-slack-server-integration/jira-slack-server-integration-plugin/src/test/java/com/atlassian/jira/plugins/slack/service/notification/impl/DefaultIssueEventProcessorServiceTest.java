package com.atlassian.jira.plugins.slack.service.notification.impl;

import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.comments.Comment;
import com.atlassian.jira.plugins.slack.dao.ConfigurationDAO;
import com.atlassian.jira.plugins.slack.manager.ProjectConfigurationManager;
import com.atlassian.jira.plugins.slack.manager.impl.DefaultProjectConfigurationManager;
import com.atlassian.jira.plugins.slack.model.EventFilterType;
import com.atlassian.jira.plugins.slack.model.EventMatcherType;
import com.atlassian.jira.plugins.slack.model.ProjectConfiguration;
import com.atlassian.jira.plugins.slack.model.event.JiraIssueEvent;
import com.atlassian.jira.plugins.slack.service.issuefilter.IssueFilterService;
import com.atlassian.jira.plugins.slack.service.notification.NotificationInfo;
import com.atlassian.jira.project.Project;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.notification.Verbosity;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import io.atlassian.fugue.Either;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class DefaultIssueEventProcessorServiceTest {
    @Mock
    private ConfigurationDAO configurationDAO;
    @Mock
    private ProjectConfigurationManager projectConfigurationManager;
    @Mock
    private IssueFilterService filterService;
    @Mock
    private SlackLinkManager slackLinkManager;

    @Mock
    private IssueEvent issueEvent;
    @Mock
    private JiraIssueEvent event;
    @Mock
    private Issue issue;
    @Mock
    private Project project;
    @Mock
    private Comment comment;
    @Mock
    private SlackLink link;
    @Mock
    private ProjectConfiguration projectConfiguration1;
    @Mock
    private ProjectConfiguration projectConfiguration2;
    @Mock
    private ProjectConfiguration projectConfiguration3;
    @Mock
    private ProjectConfiguration projectConfiguration4;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private DefaultIssueEventProcessorService target;

    @Test
    public void getNotificationsFor_shouldReturnNotifications_whenIssueIsCreated() {
        when(issue.getProjectObject()).thenReturn(project);
        when(project.getId()).thenReturn(7L);
        when(event.getEventMatcher()).thenReturn(EventMatcherType.ISSUE_CREATED);
        when(event.getIssue()).thenReturn(issue);
        when(configurationDAO.findByProjectId(7L)).thenReturn(Arrays.asList(projectConfiguration1,
                projectConfiguration2, projectConfiguration3, projectConfiguration4));
        when(projectConfiguration1.getConfigurationGroupId()).thenReturn("G");
        when(projectConfiguration1.getName()).thenReturn(EventMatcherType.ISSUE_CREATED.getDbKey());
        when(projectConfiguration1.getProjectId()).thenReturn(7L);
        when(projectConfiguration1.getChannelId()).thenReturn("C1");
        when(projectConfiguration1.getTeamId()).thenReturn("T");
        when(projectConfiguration2.getConfigurationGroupId()).thenReturn("G");
        when(projectConfiguration2.getName()).thenReturn(null);
        when(projectConfiguration2.getChannelId()).thenReturn("C1");
        when(projectConfiguration3.getConfigurationGroupId()).thenReturn("G2");
        when(projectConfiguration3.getName()).thenReturn(EventMatcherType.ISSUE_CREATED.getDbKey());
        when(projectConfiguration3.getProjectId()).thenReturn(7L);
        when(projectConfiguration3.getChannelId()).thenReturn("C2");
        when(projectConfiguration3.getTeamId()).thenReturn("T");
        when(projectConfiguration4.getConfigurationGroupId()).thenReturn("G2");
        when(projectConfiguration4.getName()).thenReturn(EventFilterType.ISSUE_TYPE.getDbKey());
        when(projectConfiguration4.getChannelId()).thenReturn("C2");
        when(configurationDAO.findByProjectConfigurationGroupId(7L, "G"))
                .thenReturn(Arrays.asList(projectConfiguration1, projectConfiguration2));
        when(configurationDAO.findByProjectConfigurationGroupId(7L, "G2"))
                .thenReturn(Arrays.asList(projectConfiguration3, projectConfiguration4));
        when(filterService.apply(event, Collections.singletonList(projectConfiguration4))).thenReturn(true);
        when(projectConfigurationManager.getOwner(projectConfiguration1)).thenReturn(Optional.of("O1"));
        when(projectConfigurationManager.getOwner(projectConfiguration3)).thenReturn(Optional.of("O2"));
        when(projectConfigurationManager.getVerbosity(projectConfiguration1)).thenReturn(Optional.of(Verbosity.BASIC));
        when(projectConfigurationManager.getVerbosity(projectConfiguration3)).thenReturn(Optional.empty());
        when(slackLinkManager.getLinkByTeamId("T")).thenReturn(Either.right(link));

        List<NotificationInfo> result = target.getNotificationsFor(event);

        assertThat(result, hasSize(2));
        assertThat(result.get(0).getChannelId(), is("C1"));
        assertThat(result.get(0).getLink(), sameInstance(link));
        assertThat(result.get(0).getConfigurationOwner(), is("O1"));
        assertThat(result.get(0).getVerbosity(), is(Verbosity.BASIC));
        assertThat(result.get(1).getChannelId(), is("C2"));
        assertThat(result.get(1).getLink(), sameInstance(link));
        assertThat(result.get(1).getConfigurationOwner(), is("O2"));
        assertThat(result.get(1).getVerbosity(), is(Verbosity.EXTENDED));
    }

    @Test
    public void getNotificationsFor_shouldReturnNothing_whenCommentIsRestricted() {
        long projectId = 7L;
        String groupId = "G";

        when(event.getEventMatcher()).thenReturn(EventMatcherType.ISSUE_COMMENTED);
        when(event.getIssue()).thenReturn(issue);
        when(event.getComment()).thenReturn(Optional.of(comment));
        when(issue.getProjectObject()).thenReturn(project);
        when(project.getId()).thenReturn(projectId);
        when(comment.getGroupLevel()).thenReturn("someGroupLevel");
        when(configurationDAO.findByProjectId(projectId)).thenReturn(Arrays.asList(projectConfiguration1,
                projectConfiguration2));
        when(projectConfiguration1.getConfigurationGroupId()).thenReturn(groupId);
        when(projectConfiguration1.getName()).thenReturn(EventMatcherType.ISSUE_COMMENTED.getDbKey());
        when(projectConfiguration1.getProjectId()).thenReturn(projectId);
        when(projectConfiguration1.getChannelId()).thenReturn("C1");
        when(projectConfiguration1.getTeamId()).thenReturn("T");
        when(projectConfiguration2.getConfigurationGroupId()).thenReturn(groupId);
        when(projectConfiguration2.getName()).thenReturn(DefaultProjectConfigurationManager.SKIP_RESTRICTED_COMMENTS);
        when(projectConfiguration2.getChannelId()).thenReturn("C1");
        when(configurationDAO.findByProjectConfigurationGroupId(projectId, groupId))
                .thenReturn(Arrays.asList(projectConfiguration1, projectConfiguration2));

        List<NotificationInfo> result = target.getNotificationsFor(event);

        assertThat(result, empty());
    }
}
