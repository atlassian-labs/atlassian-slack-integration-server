package com.atlassian.jira.plugins.slack.manager.impl;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.comments.Comment;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.plugins.slack.dao.DedicatedChannelDAO;
import com.atlassian.jira.plugins.slack.manager.ProjectConfigurationManager;
import com.atlassian.jira.plugins.slack.model.DedicatedChannel;
import com.atlassian.jira.plugins.slack.model.EventMatcherType;
import com.atlassian.jira.plugins.slack.model.event.DedicatedChannelLinkedEvent;
import com.atlassian.jira.plugins.slack.model.event.DedicatedChannelUnlinkedEvent;
import com.atlassian.jira.plugins.slack.model.event.JiraIssueEvent;
import com.atlassian.jira.plugins.slack.service.notification.NotificationInfo;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugins.slack.analytics.AnalyticsContext;
import com.atlassian.plugins.slack.analytics.AnalyticsContextProvider;
import com.atlassian.plugins.slack.api.ConversationKey;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.client.SlackClient;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.atlassian.plugins.slack.api.webhooks.ChannelDeletedSlackEvent;
import com.atlassian.plugins.slack.api.webhooks.SlackEvent;
import com.atlassian.plugins.slack.event.SlackTeamUnlinkedEvent;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.plugins.slack.util.DigestUtil;
import com.atlassian.plugins.slack.util.ErrorResponse;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.UrlMode;
import com.github.seratch.jslack.api.model.Conversation;
import io.atlassian.fugue.Either;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.Optional;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class DefaultDedicatedChannelManagerTest {
    @Mock(name = "salApplicationProperties")
    private ApplicationProperties salApplicationProperties;
    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private SlackLinkManager slackLinkManager;
    @Mock
    private SlackClientProvider slackClientProvider;
    @Mock
    private JiraAuthenticationContext jiraAuthenticationContext;
    @Mock
    private DedicatedChannelDAO dedicatedChannelDAO;
    @Mock
    private PermissionManager permissionManager;
    @Mock
    private AnalyticsContextProvider analyticsContextProvider;

    @Mock
    private DedicatedChannel dedicatedChannel;
    @Mock
    private JiraIssueEvent jiraIssueEvent;
    @Mock
    private Issue issue;
    @Mock
    private SlackLink link;
    @Mock
    private ChannelDeletedSlackEvent channelDeletedSlackEvent;
    @Mock
    private SlackTeamUnlinkedEvent slackTeamUnlinkedEvent;
    @Mock
    private ApplicationUser applicationUser;
    @Mock
    private SlackClient slackClient;
    @Mock
    private Conversation conversation;
    @Mock
    private AnalyticsContext analyticsContext;
    @Mock
    private ProjectConfigurationManager projectConfigurationManager;
    @Mock
    private SlackEvent slackEvent;

    @Captor
    private ArgumentCaptor<DedicatedChannelUnlinkedEvent> channelUnlinkedCaptor;
    @Captor
    private ArgumentCaptor<DedicatedChannelLinkedEvent> channelLinkedCaptor;
    @Captor
    private ArgumentCaptor<DedicatedChannel> dedicatedChannelCaptor;

    @InjectMocks
    private DefaultDedicatedChannelManager target;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Test
    public void getNotificationsFor() {
        IssueEvent issueEvent = new IssueEvent(issue, Collections.emptyMap(), null, 1L);
        when(jiraIssueEvent.getEventMatcher()).thenReturn(EventMatcherType.ISSUE_COMMENTED);
        when(jiraIssueEvent.getIssue()).thenReturn(issue);
        when(issue.getId()).thenReturn(7L);
        when(dedicatedChannelDAO.getDedicatedChannel(7L)).thenReturn(Optional.of(dedicatedChannel));
        when(dedicatedChannel.getTeamId()).thenReturn("T");
        when(dedicatedChannel.getChannelId()).thenReturn("C");
        when(dedicatedChannel.getCreator()).thenReturn("CR");
        when(slackLinkManager.getLinkByTeamId("T")).thenReturn(Either.right(link));

        Optional<NotificationInfo> result = target.getNotificationsFor(jiraIssueEvent);

        assertThat(result.isPresent(), is(true));
        assertThat(result.get().getLink(), sameInstance(link));
        assertThat(result.get().getChannelId(), is("C"));
        assertThat(result.get().getConfigurationOwner(), is("CR"));
    }

    @Test
    public void getNotificationsFor_shouldReturnEmptyForNonMatchingEvent() {
        when(jiraIssueEvent.getEventMatcher()).thenReturn(EventMatcherType.ISSUE_CREATED);

        Optional<NotificationInfo> result = target.getNotificationsFor(jiraIssueEvent);

        assertThat(result.isPresent(), is(false));
    }

    @Test
    public void getNotificationsFor_shouldReturnEmptyForRestrictedCommentEventAndDefaultOptions() {
        IssueEvent issueEvent = mock(IssueEvent.class);
        Comment comment = mock(Comment.class);
        Project project = mock(Project.class);
        when(jiraIssueEvent.getEventMatcher()).thenReturn(EventMatcherType.ISSUE_COMMENTED);
        when(jiraIssueEvent.getIssue()).thenReturn(issue);
        when(issueEvent.getIssue()).thenReturn(issue);
        when(issueEvent.getComment()).thenReturn(comment);
        when(issue.getProjectObject()).thenReturn(project);
        when(comment.getGroupLevel()).thenReturn("someGroupLevel");

        Optional<NotificationInfo> result = target.getNotificationsFor(jiraIssueEvent);

        assertThat(result.isPresent(), is(false));
    }

    @Test
    public void getNotificationsFor_shouldReturnNotificationsForRestrictedCommentEventAndEnabledOption() {
        IssueEvent issueEvent = mock(IssueEvent.class);
        Comment comment = mock(Comment.class);
        Project project = mock(Project.class);
        when(jiraIssueEvent.getEventMatcher()).thenReturn(EventMatcherType.ISSUE_COMMENTED);
        when(jiraIssueEvent.getIssue()).thenReturn(issue);
        when(issueEvent.getIssue()).thenReturn(issue);
        when(issueEvent.getComment()).thenReturn(comment);
        when(issue.getProjectObject()).thenReturn(project);
        when(comment.getGroupLevel()).thenReturn("someGroupLevel");
        when(projectConfigurationManager.shouldSendRestrictedCommentsToDedicatedChannels(project)).thenReturn(true);

        Optional<NotificationInfo> result = target.getNotificationsFor(jiraIssueEvent);

        assertThat(result.isPresent(), is(false));
    }

    @Test
    public void onChannelDeletedEvent() {
        when(channelDeletedSlackEvent.getChannel()).thenReturn("C");
        when(channelDeletedSlackEvent.getSlackEvent()).thenReturn(slackEvent);
        when(slackEvent.getTeamId()).thenReturn("T");
        when(dedicatedChannelDAO.findMappingsForChannel(new ConversationKey("T", "C"))).thenReturn(Collections.singletonList(dedicatedChannel));
        when(dedicatedChannel.getIssueId()).thenReturn(1L);

        target.onChannelDeletedEvent(channelDeletedSlackEvent);

        verify(dedicatedChannelDAO).deleteDedicatedChannel(1L);
    }

    @Test
    public void onTeamDisconnection() {
        when(slackTeamUnlinkedEvent.getTeamId()).thenReturn("T");
        when(dedicatedChannelDAO.findMappingsByTeamId("T")).thenReturn(Collections.singletonList(dedicatedChannel));
        when(dedicatedChannel.getIssueId()).thenReturn(1L);

        target.onTeamDisconnection(slackTeamUnlinkedEvent);

        verify(dedicatedChannelDAO).deleteDedicatedChannel(1L);
    }

    @Test
    public void assignDedicatedChannel() {
        when(jiraAuthenticationContext.getLoggedInUser()).thenReturn(applicationUser);
        when(permissionManager.hasPermission(ProjectPermissions.EDIT_ISSUES, issue, applicationUser)).thenReturn(true);
        when(slackClientProvider.withTeamId("T")).thenReturn(Either.right(slackClient));
        when(applicationUser.getKey()).thenReturn("UK");
        when(slackClient.withUserTokenIfAvailable("UK")).thenReturn(Optional.of(slackClient));
        when(slackClient.getConversationsInfo("C")).thenReturn(Either.right(conversation));
        when(conversation.getId()).thenReturn("C");
        when(conversation.getName()).thenReturn("CN");
        when(conversation.isPrivate()).thenReturn(true);
        when(issue.getId()).thenReturn(3L);
        when(issue.getKey()).thenReturn("IK");
        when(issue.getSummary()).thenReturn("IS");
        when(issue.getProjectId()).thenReturn(7L);
        when(salApplicationProperties.getBaseUrl(UrlMode.CANONICAL)).thenReturn("BASE");
        when(slackClient.selfInviteToConversation("C")).thenReturn(Either.left(new ErrorResponse(new Exception())));
        when(slackClient.setConversationTopic("C", "BASE/browse/IK - IS")).thenReturn(Either.left(new ErrorResponse(new Exception())));
        when(analyticsContextProvider.byTeamIdAndUserKey("T", "UK")).thenReturn(analyticsContext);
        when(analyticsContext.getTeamId()).thenReturn("T");

        Either<ErrorResponse, DedicatedChannel> result = target.assignDedicatedChannel(issue, "T", "C");

        assertThat(result.isRight(), is(true));
        assertThat(result.right().get().getChannelId(), is("C"));
        assertThat(result.right().get().getTeamId(), is("T"));
        assertThat(result.right().get().getIssueId(), is(3L));
        assertThat(result.right().get().getCreator(), is("UK"));
        assertThat(result.right().get().getName(), is("CN"));
        assertThat(result.right().get().isPrivateChannel(), is(true));

        verify(dedicatedChannelDAO).insertDedicatedChannel(dedicatedChannelCaptor.capture());
        verify(eventPublisher).publish(channelLinkedCaptor.capture());
        verify(slackClient).selfInviteToConversation("C");
        verify(slackClient).setConversationTopic("C", "BASE/browse/IK - IS");

        DedicatedChannelLinkedEvent event = channelLinkedCaptor.getValue();
        assertThat(event.getIssueKey(), is("IK"));
        assertThat(event.getProjectId(), is(7L));
        assertThat(event.getChannelId(), is("C"));
        assertThat(event.getTeamId(), is("T"));
        assertThat(event.getOwner(), is("UK"));
        assertThat(event.getOwnerHash(), is(DigestUtil.crc32("UK")));
        assertThat(event.getIssueKeyHash(), is(DigestUtil.crc32("IK")));
        assertThat(event.getChannelIdHash(), is(DigestUtil.crc32("C")));

        DedicatedChannel dc = dedicatedChannelCaptor.getValue();
        assertThat(result.right().get(), sameInstance(dc));

    }

    @Test
    public void assignDedicatedChannel_shouldReturnErrorIfConversationIsNotFound() {
        when(jiraAuthenticationContext.getLoggedInUser()).thenReturn(applicationUser);
        when(permissionManager.hasPermission(ProjectPermissions.EDIT_ISSUES, issue, applicationUser)).thenReturn(true);
        when(slackClientProvider.withTeamId("T")).thenReturn(Either.right(slackClient));
        when(applicationUser.getKey()).thenReturn("UK");
        when(slackClient.withUserTokenIfAvailable("UK")).thenReturn(Optional.of(slackClient));
        when(slackClient.getConversationsInfo("C")).thenReturn(Either.left(new ErrorResponse(new Exception())));

        Either<ErrorResponse, DedicatedChannel> result = target.assignDedicatedChannel(issue, "T", "C");

        assertThat(result.isLeft(), is(true));
        assertThat(result.left().get().getStatusCode(), is(BAD_REQUEST.getStatusCode()));
    }

    @Test
    public void assignDedicatedChannel_shouldReturnErrorIfUserHasNotConfirmedAccount() {
        when(jiraAuthenticationContext.getLoggedInUser()).thenReturn(applicationUser);
        when(permissionManager.hasPermission(ProjectPermissions.EDIT_ISSUES, issue, applicationUser)).thenReturn(true);
        when(slackClientProvider.withTeamId("T")).thenReturn(Either.right(slackClient));
        when(applicationUser.getKey()).thenReturn("UK");
        when(slackClient.withUserTokenIfAvailable("UK")).thenReturn(Optional.empty());

        Either<ErrorResponse, DedicatedChannel> result = target.assignDedicatedChannel(issue, "T", "C");

        assertThat(result.isLeft(), is(true));
        assertThat(result.left().get().getStatusCode(), is(BAD_REQUEST.getStatusCode()));
    }

    @Test
    public void assignDedicatedChannel_shouldReturnErrorIfNotLoggedIn() {
        when(jiraAuthenticationContext.getLoggedInUser()).thenReturn(null);

        Either<ErrorResponse, DedicatedChannel> result = target.assignDedicatedChannel(issue, "", "");

        assertThat(result.isLeft(), is(true));
        assertThat(result.left().get().getStatusCode(), is(FORBIDDEN.getStatusCode()));
    }

    @Test
    public void assignDedicatedChannel_shouldReturnErrorIfUserHasNoPermission() {
        when(jiraAuthenticationContext.getLoggedInUser()).thenReturn(applicationUser);
        when(permissionManager.hasPermission(ProjectPermissions.EDIT_ISSUES, issue, applicationUser)).thenReturn(false);

        Either<ErrorResponse, DedicatedChannel> result = target.assignDedicatedChannel(issue, "", "");

        assertThat(result.isLeft(), is(true));
        assertThat(result.left().get().getStatusCode(), is(FORBIDDEN.getStatusCode()));
    }

    @Test
    public void unassignDedicatedChannel() {
        when(jiraAuthenticationContext.getLoggedInUser()).thenReturn(applicationUser);
        when(permissionManager.hasPermission(ProjectPermissions.EDIT_ISSUES, issue, applicationUser)).thenReturn(true);
        when(issue.getId()).thenReturn(1L);
        when(issue.getProjectId()).thenReturn(7L);
        when(issue.getKey()).thenReturn("IK");
        when(dedicatedChannel.getChannelId()).thenReturn("C");
        when(dedicatedChannel.getTeamId()).thenReturn("T");
        when(dedicatedChannel.getCreator()).thenReturn("O");
        when(dedicatedChannelDAO.getDedicatedChannel(1L)).thenReturn(Optional.of(dedicatedChannel));
        when(analyticsContextProvider.byTeamIdAndUserKey("T", "O")).thenReturn(analyticsContext);
        when(analyticsContext.getTeamId()).thenReturn("T");

        Optional<ErrorResponse> result = target.unassignDedicatedChannel(issue);

        assertThat(result.isPresent(), is(false));
        verify(dedicatedChannelDAO).deleteDedicatedChannel(1L);
        verify(eventPublisher).publish(channelUnlinkedCaptor.capture());

        DedicatedChannelUnlinkedEvent event = channelUnlinkedCaptor.getValue();
        assertThat(event.getIssueKey(), is("IK"));
        assertThat(event.getProjectId(), is(7L));
        assertThat(event.getChannelId(), is("C"));
        assertThat(event.getTeamId(), is("T"));
        assertThat(event.getOwner(), is("O"));
        assertThat(event.getOwnerHash(), is(DigestUtil.crc32("O")));
        assertThat(event.getIssueKeyHash(), is(DigestUtil.crc32("IK")));
        assertThat(event.getChannelIdHash(), is(DigestUtil.crc32("C")));
        assertThat(event.getTeamIdHash(), is(DigestUtil.crc32("T")));
    }

    @Test
    public void unassignDedicatedChannel_shouldDoNothingIsDedicatedChannelDoesNotExist() {
        when(jiraAuthenticationContext.getLoggedInUser()).thenReturn(applicationUser);
        when(permissionManager.hasPermission(ProjectPermissions.EDIT_ISSUES, issue, applicationUser)).thenReturn(true);
        when(issue.getId()).thenReturn(1L);
        when(dedicatedChannelDAO.getDedicatedChannel(1L)).thenReturn(Optional.empty());

        Optional<ErrorResponse> result = target.unassignDedicatedChannel(issue);

        assertThat(result.isPresent(), is(false));
        verify(dedicatedChannelDAO, never()).deleteDedicatedChannel(anyLong());
    }

    @Test
    public void unassignDedicatedChannel_shouldReturnErrorIfNotLoggedIn() {
        when(jiraAuthenticationContext.getLoggedInUser()).thenReturn(null);

        Optional<ErrorResponse> result = target.unassignDedicatedChannel(issue);

        assertThat(result.isPresent(), is(true));
        assertThat(result.get().getStatusCode(), is(FORBIDDEN.getStatusCode()));
    }

    @Test
    public void unassignDedicatedChannel_shouldReturnErrorIfUserHasNoPermission() {
        when(jiraAuthenticationContext.getLoggedInUser()).thenReturn(applicationUser);
        when(permissionManager.hasPermission(ProjectPermissions.EDIT_ISSUES, issue, applicationUser)).thenReturn(false);

        Optional<ErrorResponse> result = target.unassignDedicatedChannel(issue);

        assertThat(result.isPresent(), is(true));
        assertThat(result.get().getStatusCode(), is(FORBIDDEN.getStatusCode()));
    }

    @Test
    public void getDedicatedChannel() {
        when(issue.getId()).thenReturn(1L);
        when(dedicatedChannelDAO.getDedicatedChannel(1L)).thenReturn(Optional.of(dedicatedChannel));

        Optional<DedicatedChannel> result = target.getDedicatedChannel(issue);

        assertThat(result, is(Optional.of(dedicatedChannel)));
    }

    @Test
    public void canAssignDedicatedChannel() {
        when(jiraAuthenticationContext.getLoggedInUser()).thenReturn(applicationUser);
        when(permissionManager.hasPermission(ProjectPermissions.EDIT_ISSUES, issue, applicationUser)).thenReturn(true);

        boolean result = target.canAssignDedicatedChannel(issue);

        assertThat(result, is(true));
    }

    @Test
    public void isNotSameChannel_shouldReturnFalseForSameChannel() {
        when(dedicatedChannel.getChannelId()).thenReturn("C");
        when(dedicatedChannel.getTeamId()).thenReturn("T");
        boolean result = target.isNotSameChannel(new ConversationKey("T", "C"), Optional.of(dedicatedChannel));
        assertThat(result, is(false));
    }

    @Test
    public void isNotSameChannel_shouldReturnTrueForDifferentChannel() {
        when(dedicatedChannel.getTeamId()).thenReturn("T");
        when(dedicatedChannel.getChannelId()).thenReturn("C");
        boolean result = target.isNotSameChannel(new ConversationKey("T2", "C2"), Optional.of(dedicatedChannel));
        assertThat(result, is(true));
    }

    @Test
    public void isNotSameChannel_shouldReturnFalseForEmptyChannel() {
        boolean result = target.isNotSameChannel(new ConversationKey("T2", "C2"), Optional.empty());
        assertThat(result, is(false));
    }
}
