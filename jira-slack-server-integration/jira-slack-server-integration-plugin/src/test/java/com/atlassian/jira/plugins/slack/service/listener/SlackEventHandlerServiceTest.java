package com.atlassian.jira.plugins.slack.service.listener;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.plugins.slack.manager.DedicatedChannelManager;
import com.atlassian.jira.plugins.slack.manager.IssueDetailsMessageManager;
import com.atlassian.jira.plugins.slack.manager.PluginConfigurationManager;
import com.atlassian.jira.plugins.slack.manager.ProjectConfigurationManager;
import com.atlassian.jira.plugins.slack.mentions.storage.cache.MentionChannelCacheManager;
import com.atlassian.jira.plugins.slack.model.ChannelKeyImpl;
import com.atlassian.jira.plugins.slack.model.DedicatedChannel;
import com.atlassian.jira.plugins.slack.model.SlackIncomingMessage;
import com.atlassian.jira.plugins.slack.model.analytics.DedicatedChannelIssueMentionedEvent;
import com.atlassian.jira.plugins.slack.model.event.IssueMentionedEvent;
import com.atlassian.jira.plugins.slack.model.event.ShowIssueEvent;
import com.atlassian.jira.plugins.slack.model.event.UnauthorizedUnfurlEvent;
import com.atlassian.jira.plugins.slack.model.mentions.MentionChannel;
import com.atlassian.jira.plugins.slack.service.notification.NotificationInfo;
import com.atlassian.jira.plugins.slack.service.task.TaskBuilder;
import com.atlassian.jira.plugins.slack.service.task.TaskExecutorService;
import com.atlassian.jira.plugins.slack.service.task.impl.DirectMessageTask;
import com.atlassian.jira.plugins.slack.service.task.impl.ProcessIssueMentionTask;
import com.atlassian.jira.plugins.slack.service.task.impl.SendNotificationTask;
import com.atlassian.jira.plugins.slack.service.task.impl.UnfurlIssueLinksTask;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.plugins.slack.analytics.AnalyticsContext;
import com.atlassian.plugins.slack.analytics.AnalyticsContextProvider;
import com.atlassian.plugins.slack.api.ConversationKey;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.SlackUser;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.plugins.slack.user.SlackUserManager;
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

import java.util.Arrays;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SlackEventHandlerServiceTest {
    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private TaskExecutorService taskExecutorService;
    @Mock
    private TaskBuilder taskBuilder;
    @Mock
    private IssueManager issueManager;
    @Mock
    private ApplicationProperties applicationProperties;
    @Mock
    private ProjectConfigurationManager projectConfigurationManager;
    @Mock
    private IssueDetailsMessageManager issueDetailsMessageManager;
    @Mock
    private DedicatedChannelManager dedicatedChannelManager;
    @Mock
    private SlackLinkManager slackLinkManager;
    @Mock
    private MentionChannelCacheManager mentionChannelCacheManager;
    @Mock
    private PluginConfigurationManager pluginConfigurationManager;
    @Mock
    private SlackUserManager slackUserManager;
    @Mock
    private UserManager userManager;
    @Mock
    private PermissionManager permissionManager;
    @Mock
    private AnalyticsContextProvider analyticsContextProvider;

    @Mock
    private SlackIncomingMessage message;
    @Mock
    private SlackLink link;
    @Mock
    private SlackLink link2;
    @Mock
    private Conversation conversation;
    @Mock
    private UnfurlIssueLinksTask unfurlIssueLinksTask;

    @Mock
    private ApplicationUser applicationUser;
    @Mock
    private MutableIssue issue1;
    @Mock
    private MutableIssue issue2;
    @Mock
    private MutableIssue issue3;
    @Mock
    private MutableIssue issueDedicated;
    @Mock
    private DedicatedChannel dedicatedChannel;
    @Mock
    private DirectMessageTask directMessageTask;
    @Mock
    private ProcessIssueMentionTask processIssueMentionTask;
    @Mock
    private SlackUser slackUser;
    @Mock
    private Project project;
    @Mock
    private SendNotificationTask sendNotificationTask;
    @Mock
    private AnalyticsContext context;

    @Captor
    private ArgumentCaptor<NotificationInfo> notificationInfoCaptor;
    @Captor
    private ArgumentCaptor<NotificationInfo> notificationInfoCaptorDedicated;
    @Captor
    private ArgumentCaptor<IssueMentionedEvent> issueMentionedEventArgumentCaptor;
    @Captor
    private ArgumentCaptor<ShowIssueEvent> showIssueEventArgumentCaptor;
    @Captor
    private ArgumentCaptor<UnauthorizedUnfurlEvent> unauthorizedUnfurlEventArgumentCaptor;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private SlackEventHandlerService target;

    @Test
    public void handleMessage_shouldDoNothingIfUserIsBot() {
        when(message.getSlackLink()).thenReturn(link);
        when(message.getUser()).thenReturn("B");
        when(link.getBotUserId()).thenReturn("B");

        boolean result = target.handleMessage(message);

        assertThat(result, is(false));
    }

    @Test
    public void handleMessage_shouldPostNotificationForNewIssueKeys() {
        when(message.getSlackLink()).thenReturn(link);
        when(message.getTeamId()).thenReturn("T");
        when(message.getChannelId()).thenReturn("C");
        when(message.getUser()).thenReturn("U");
        when(message.getText()).thenReturn("ISS-2 and ISS-1 and http://jira.com/browse/ISS-3 ISS-4");
        when(message.getPreviousText()).thenReturn("ISS-2");
        when(message.getResponseUrl()).thenReturn("url");
        when(message.getThreadTs()).thenReturn("tts");
        when(message.isMessageEdit()).thenReturn(true);
        when(link.getBotUserId()).thenReturn("B");
        when(applicationProperties.getBaseUrl(UrlMode.CANONICAL)).thenReturn("http://jira.com");
        when(slackUser.getUserKey()).thenReturn("JU");

        final ChannelKeyImpl key = new ChannelKeyImpl("JU", "T", "C");
        when(mentionChannelCacheManager.get(key)).thenReturn(Optional.of(new MentionChannel(key, conversation, "")));

        when(slackLinkManager.shouldUseLinkUnfurl("T")).thenReturn(true);
        when(taskBuilder.newUnfurlIssueLinksTask()).thenReturn(unfurlIssueLinksTask);
        when(issueManager.getIssueByCurrentKey("ISS-1")).thenReturn(issue1);
        when(issueManager.getIssueByCurrentKey("ISS-2")).thenReturn(issue2);
        when(issueManager.getIssueByCurrentKey("ISS-3")).thenReturn(issue3);
        when(issueManager.getIssueByCurrentKey("ISS-4")).thenReturn(null);
        when(issue1.getProjectObject()).thenReturn(project);

        when(dedicatedChannelManager.getDedicatedChannel(issue1)).thenReturn(Optional.empty());
        when(dedicatedChannelManager.getDedicatedChannel(issue2)).thenReturn(Optional.empty());

        when(slackUserManager.getBySlackUserId("U")).thenReturn(Optional.of(slackUser));
        when(userManager.getUserByKey("JU")).thenReturn(applicationUser);
        when(permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, issue1, applicationUser)).thenReturn(true);
        when(permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, issue2, applicationUser)).thenReturn(false);
        when(taskBuilder.newProcessIssueMentionTask(issue1, message)).thenReturn(processIssueMentionTask);
        when(projectConfigurationManager.isProjectAutoConvertEnabled(project)).thenReturn(true);

        boolean result = target.handleMessage(message);

        assertThat(result, is(true));

        verify(taskExecutorService).submitTask(processIssueMentionTask);
        verify(issueDetailsMessageManager).sendIssueDetailsMessageToChannel(
                notificationInfoCaptor.capture(), same(issue1), isNull());

        NotificationInfo notifInfo = notificationInfoCaptor.getValue();
        assertThat(notifInfo.getLink(), sameInstance(link));
        assertThat(notifInfo.getChannelId(), is("C"));
        assertThat(notifInfo.getResponseUrl(), is("url"));
        assertThat(notifInfo.getThreadTimestamp(), is("tts"));
    }

    @Test
    public void handleMessage_shouldNotifyDedicatedChannel() {
        when(message.getSlackLink()).thenReturn(link);
        when(message.getTeamId()).thenReturn("T");
        when(message.getChannelId()).thenReturn("C");
        when(message.getUser()).thenReturn("U");
        when(message.getText()).thenReturn("DED-1 a");
        when(message.getResponseUrl()).thenReturn("url");
        when(message.getThreadTs()).thenReturn("tts");
        when(link.getBotUserId()).thenReturn("B");
        when(slackUser.getUserKey()).thenReturn("JU");

        final ChannelKeyImpl key = new ChannelKeyImpl("JU", "T", "C");
        when(mentionChannelCacheManager.get(key)).thenReturn(Optional.of(new MentionChannel(key, conversation, "")));

        when(slackLinkManager.shouldUseLinkUnfurl("T")).thenReturn(true);
        when(taskBuilder.newUnfurlIssueLinksTask()).thenReturn(unfurlIssueLinksTask);
        when(issueManager.getIssueByCurrentKey("DED-1")).thenReturn(issueDedicated);
        when(issueDedicated.getProjectObject()).thenReturn(project);

        when(dedicatedChannelManager.getDedicatedChannel(issueDedicated)).thenReturn(Optional.of(dedicatedChannel));

        when(slackUserManager.getBySlackUserId("U")).thenReturn(Optional.of(slackUser));
        when(userManager.getUserByKey("JU")).thenReturn(applicationUser);
        when(permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, issueDedicated, applicationUser)).thenReturn(true);
        when(taskBuilder.newProcessIssueMentionTask(issueDedicated, message)).thenReturn(processIssueMentionTask);
        when(projectConfigurationManager.isProjectAutoConvertEnabled(project)).thenReturn(true);
        when(dedicatedChannelManager.isNotSameChannel(new ConversationKey("T", "C"), Optional.of(dedicatedChannel))).thenReturn(true);
        when(dedicatedChannel.getTeamId()).thenReturn("T2");
        when(dedicatedChannel.getChannelId()).thenReturn("C2");
        when(dedicatedChannel.getCreator()).thenReturn("CR");
        when(dedicatedChannel.getIssueId()).thenReturn(3L);
        when(slackLinkManager.getLinkByTeamId("T2")).thenReturn(Either.right(link2));
        when(taskBuilder.newSendNotificationTask(
                issueMentionedEventArgumentCaptor.capture(),
                notificationInfoCaptorDedicated.capture(),
                same(taskExecutorService))
        ).thenReturn(sendNotificationTask);

        boolean result = target.handleMessage(message);

        assertThat(result, is(true));

        verify(taskExecutorService).submitTask(processIssueMentionTask);
        verify(taskExecutorService).submitTask(sendNotificationTask);
        verify(eventPublisher).publish(isA(DedicatedChannelIssueMentionedEvent.class));
        verify(issueDetailsMessageManager).sendIssueDetailsMessageToChannel(
                notificationInfoCaptor.capture(), same(issueDedicated), same(dedicatedChannel));

        NotificationInfo notifInfo = notificationInfoCaptor.getValue();
        assertThat(notifInfo.getLink(), sameInstance(link));
        assertThat(notifInfo.getChannelId(), is("C"));
        assertThat(notifInfo.getResponseUrl(), is("url"));
        assertThat(notifInfo.getThreadTimestamp(), is("tts"));

        NotificationInfo notifInfo2 = notificationInfoCaptorDedicated.getValue();
        assertThat(notifInfo2.getLink(), sameInstance(link2));
        assertThat(notifInfo2.getChannelId(), is("C2"));
        assertThat(notifInfo2.getConfigurationOwner(), is("CR"));

        IssueMentionedEvent issueMentionedEvent = issueMentionedEventArgumentCaptor.getValue();
        assertThat(issueMentionedEvent.getIssueId(), is(3L));
        assertThat(issueMentionedEvent.getMessage(), sameInstance(message));
    }

    @Test
    public void handleMessage_shouldUseUnfurl() {
        when(message.getSlackLink()).thenReturn(link);
        when(message.getTeamId()).thenReturn("T");
        when(message.getChannelId()).thenReturn("C");
        when(message.getUser()).thenReturn("U");
        when(message.getText()).thenReturn("x (http://jira.com/browse/ISS-1) v");
        when(message.getPreviousText()).thenReturn(null);
        when(message.getResponseUrl()).thenReturn("url");
        when(message.getThreadTs()).thenReturn("tts");
        when(message.getTs()).thenReturn("ts");
        when(message.isLinkShared()).thenReturn(true);
        when(link.getBotUserId()).thenReturn("B");
        when(applicationProperties.getBaseUrl(UrlMode.CANONICAL)).thenReturn("http://jira.com");
        when(slackUser.getUserKey()).thenReturn("JU");

        final ChannelKeyImpl key = new ChannelKeyImpl("JU", "T", "C");
        when(mentionChannelCacheManager.get(key)).thenReturn(Optional.of(new MentionChannel(key, conversation, "")));

        when(slackLinkManager.shouldUseLinkUnfurl("T")).thenReturn(false);
        when(taskBuilder.newUnfurlIssueLinksTask()).thenReturn(unfurlIssueLinksTask);
        when(issueManager.getIssueByCurrentKey("ISS-1")).thenReturn(issue1);
        when(issue1.getProjectObject()).thenReturn(project);

        when(dedicatedChannelManager.getDedicatedChannel(issue1)).thenReturn(Optional.empty());

        when(slackUserManager.getBySlackUserId("U")).thenReturn(Optional.of(slackUser));
        when(userManager.getUserByKey("JU")).thenReturn(applicationUser);
        when(permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, issue1, applicationUser)).thenReturn(true);
        when(taskBuilder.newProcessIssueMentionTask(issue1, message)).thenReturn(processIssueMentionTask);
        when(projectConfigurationManager.isProjectAutoConvertEnabled(project)).thenReturn(true);

        boolean result = target.handleMessage(message);

        assertThat(result, is(true));

        verify(taskExecutorService).submitTask(processIssueMentionTask);
        verify(taskExecutorService).submitTask(unfurlIssueLinksTask);
        verify(issueDetailsMessageManager, never()).sendIssueDetailsMessageToChannel(any(), any(), any());

        verify(unfurlIssueLinksTask).addNotification(showIssueEventArgumentCaptor.capture(), notificationInfoCaptor.capture());

        NotificationInfo notifInfo = notificationInfoCaptor.getValue();
        assertThat(notifInfo.getLink(), sameInstance(link));
        assertThat(notifInfo.getChannelId(), is("C"));
        assertThat(notifInfo.getResponseUrl(), is("url"));
        assertThat(notifInfo.getThreadTimestamp(), is("tts"));
        assertThat(notifInfo.getMessageTimestamp(), is("ts"));
        assertThat(notifInfo.getMessageAuthorId(), is("U"));
        assertThat(notifInfo.getIssueUrl(), is("http://jira.com/browse/ISS-1"));
    }

    @Test
    public void handleMessage_shouldInviteNotLoggedUser() {
        when(message.getSlackLink()).thenReturn(link);
        when(message.getTeamId()).thenReturn("T");
        when(message.getChannelId()).thenReturn("C");
        when(message.getUser()).thenReturn("U");
        when(message.getText()).thenReturn("x (http://jira.com/browse/ISS-1) v");
        when(message.getPreviousText()).thenReturn(null);
        when(message.getResponseUrl()).thenReturn("url");
        when(message.getThreadTs()).thenReturn("tts");
        when(message.getTs()).thenReturn("ts");
        when(message.isLinkShared()).thenReturn(true);
        when(link.getBotUserId()).thenReturn("B");
        when(applicationProperties.getBaseUrl(UrlMode.CANONICAL)).thenReturn("http://jira.com");

        final ChannelKeyImpl key = new ChannelKeyImpl("", "T", "C");
        when(mentionChannelCacheManager.get(key)).thenReturn(Optional.of(new MentionChannel(key, conversation, "")));

        when(slackLinkManager.shouldUseLinkUnfurl("T")).thenReturn(false);
        when(taskBuilder.newUnfurlIssueLinksTask()).thenReturn(unfurlIssueLinksTask);
        when(issueManager.getIssueByCurrentKey("ISS-1")).thenReturn(issue1);
        when(issue1.getProjectId()).thenReturn(7L);
        when(issue1.getKey()).thenReturn("K");
        when(issue1.getProjectObject()).thenReturn(project);

        when(taskBuilder.newDirectMessageTask(unauthorizedUnfurlEventArgumentCaptor.capture(), notificationInfoCaptor.capture()))
                .thenReturn(directMessageTask);
        when(dedicatedChannelManager.getDedicatedChannel(issue1)).thenReturn(Optional.empty());
        when(slackUserManager.getBySlackUserId("U")).thenReturn(Optional.empty());
        when(taskBuilder.newProcessIssueMentionTask(issue1, message)).thenReturn(processIssueMentionTask);
        when(analyticsContextProvider.byTeamIdAndSlackUserId("T", "U")).thenReturn(context);
        when(context.getTeamId()).thenReturn("T");
        when(projectConfigurationManager.isProjectAutoConvertEnabled(project)).thenReturn(true);

        boolean result = target.handleMessage(message);

        assertThat(result, is(false));

        verify(taskExecutorService).submitTask(directMessageTask);
        verify(issueDetailsMessageManager, never()).sendIssueDetailsMessageToChannel(any(), any(), any());
        verify(unfurlIssueLinksTask, never()).addNotification(any(), any());

        UnauthorizedUnfurlEvent event = unauthorizedUnfurlEventArgumentCaptor.getValue();
        assertThat(event.getTeamId(), is("T"));
        assertThat(event.getChannelId(), is("C"));
        assertThat(event.getProjectId(), is(7L));
        assertThat(event.getIssueKey(), is("K"));

        NotificationInfo notifInfo = notificationInfoCaptor.getValue();
        assertThat(notifInfo.getLink(), sameInstance(link));
        assertThat(notifInfo.getChannelId(), is("C"));
        assertThat(notifInfo.getResponseUrl(), is("url"));
        assertThat(notifInfo.getThreadTimestamp(), is("tts"));
        assertThat(notifInfo.getMessageTimestamp(), is("ts"));
        assertThat(notifInfo.getMessageAuthorId(), is("U"));
    }

    @Test
    public void handleMessage_willNotNotifyIfExternallyShared() {
        when(message.getSlackLink()).thenReturn(link);
        when(message.getTeamId()).thenReturn("T");
        when(message.getChannelId()).thenReturn("C");
        when(message.getUser()).thenReturn("U");
        when(message.getText()).thenReturn("DED-1 a");
        when(link.getBotUserId()).thenReturn("B");
        when(slackUser.getUserKey()).thenReturn("JU");

        final ChannelKeyImpl key = new ChannelKeyImpl("JU", "T", "C");
        when(mentionChannelCacheManager.get(key)).thenReturn(Optional.of(new MentionChannel(key, conversation, "")));

        when(pluginConfigurationManager.isIssuePreviewForGuestChannelsEnabled()).thenReturn(false);
        when(conversation.isExtShared()).thenReturn(true);
        when(slackLinkManager.shouldUseLinkUnfurl("T")).thenReturn(true);
        when(taskBuilder.newUnfurlIssueLinksTask()).thenReturn(unfurlIssueLinksTask);
        when(issueManager.getIssueByCurrentKey("DED-1")).thenReturn(issueDedicated);
        when(issueDedicated.getProjectObject()).thenReturn(project);
        when(dedicatedChannelManager.getDedicatedChannel(issueDedicated)).thenReturn(Optional.of(dedicatedChannel));
        when(slackUserManager.getBySlackUserId("U")).thenReturn(Optional.of(slackUser));
        when(userManager.getUserByKey("JU")).thenReturn(applicationUser);
        when(permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, issueDedicated, applicationUser)).thenReturn(true);
        when(projectConfigurationManager.isProjectAutoConvertEnabled(project)).thenReturn(true);
        when(dedicatedChannel.getChannelId()).thenReturn("C2");

        boolean result = target.handleMessage(message);

        assertThat(result, is(false));

        verify(taskExecutorService, never()).submitTask(any()); //sendNotificationTask);
        verify(eventPublisher, never()).publish(any());
        verify(issueDetailsMessageManager, never()).sendIssueDetailsMessageToChannel(any(), any(), any());
    }

    @Test
    public void handleMessage_shouldStoreAndNotNotifyDedicatedChannelIfPrivateChannel() {
        when(message.getSlackLink()).thenReturn(link);
        when(message.getTeamId()).thenReturn("T");
        when(message.getChannelId()).thenReturn("C");
        when(message.getUser()).thenReturn("U");
        when(message.getText()).thenReturn("DED-1 a");
        when(message.getResponseUrl()).thenReturn("url");
        when(message.getThreadTs()).thenReturn("tts");
        when(link.getBotUserId()).thenReturn("B");
        when(slackUser.getUserKey()).thenReturn("JU");

        when(slackLinkManager.shouldUseLinkUnfurl("T")).thenReturn(true);
        when(taskBuilder.newUnfurlIssueLinksTask()).thenReturn(unfurlIssueLinksTask);
        when(issueManager.getIssueByCurrentKey("DED-1")).thenReturn(issueDedicated);
        when(issueDedicated.getProjectObject()).thenReturn(project);

        when(dedicatedChannelManager.getDedicatedChannel(issueDedicated)).thenReturn(Optional.of(dedicatedChannel));

        when(slackUserManager.getBySlackUserId("U")).thenReturn(Optional.of(slackUser));
        when(userManager.getUserByKey("JU")).thenReturn(applicationUser);
        when(permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, issueDedicated, applicationUser)).thenReturn(true);
        when(projectConfigurationManager.isProjectAutoConvertEnabled(project)).thenReturn(true);
        when(dedicatedChannel.getChannelId()).thenReturn("C2");

        boolean result = target.handleMessage(message);

        assertThat(result, is(true));

        verify(taskBuilder).newProcessIssueMentionTask(any(), any());
        verify(taskBuilder, never()).newSendNotificationTask(any(), (NotificationInfo) any(), any());
        verify(eventPublisher, never()).publish(any());
        verify(issueDetailsMessageManager).sendIssueDetailsMessageToChannel(
                notificationInfoCaptor.capture(), same(issueDedicated), same(dedicatedChannel));

        NotificationInfo notifInfo = notificationInfoCaptor.getValue();
        assertThat(notifInfo.getLink(), sameInstance(link));
        assertThat(notifInfo.getChannelId(), is("C"));
        assertThat(notifInfo.getResponseUrl(), is("url"));
        assertThat(notifInfo.getThreadTimestamp(), is("tts"));
    }

    @Test
    public void handleMessage_shouldNotStoreOrNotifyDedicatedChannelIfSlashCommand() {
        when(message.getSlackLink()).thenReturn(link);
        when(message.getTeamId()).thenReturn("T");
        when(message.getChannelId()).thenReturn("C");
        when(message.getUser()).thenReturn("U");
        when(message.getText()).thenReturn("DED-1 a");
        when(message.getResponseUrl()).thenReturn("url");
        when(message.getThreadTs()).thenReturn("tts");
        when(message.isSlashCommand()).thenReturn(true);
        when(link.getBotUserId()).thenReturn("B");
        when(slackUser.getUserKey()).thenReturn("JU");

        when(slackLinkManager.shouldUseLinkUnfurl("T")).thenReturn(true);
        when(taskBuilder.newUnfurlIssueLinksTask()).thenReturn(unfurlIssueLinksTask);
        when(issueManager.getIssueByCurrentKey("DED-1")).thenReturn(issueDedicated);
        when(issueDedicated.getProjectObject()).thenReturn(project);

        when(dedicatedChannelManager.getDedicatedChannel(issueDedicated)).thenReturn(Optional.of(dedicatedChannel));

        when(slackUserManager.getBySlackUserId("U")).thenReturn(Optional.of(slackUser));
        when(userManager.getUserByKey("JU")).thenReturn(applicationUser);
        when(permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, issueDedicated, applicationUser)).thenReturn(true);
        when(projectConfigurationManager.isProjectAutoConvertEnabled(project)).thenReturn(true);
        when(dedicatedChannel.getChannelId()).thenReturn("C2");

        boolean result = target.handleMessage(message);

        assertThat(result, is(true));

        verify(taskBuilder, never()).newProcessIssueMentionTask(any(), any());
        verify(taskBuilder, never()).newSendNotificationTask(any(), (NotificationInfo) any(), any());
        verify(eventPublisher, never()).publish(any());
        verify(issueDetailsMessageManager).sendIssueDetailsMessageToChannel(
                notificationInfoCaptor.capture(), same(issueDedicated), same(dedicatedChannel));

        NotificationInfo notifInfo = notificationInfoCaptor.getValue();
        assertThat(notifInfo.getLink(), sameInstance(link));
        assertThat(notifInfo.getChannelId(), is("C"));
        assertThat(notifInfo.getResponseUrl(), is("url"));
        assertThat(notifInfo.getThreadTimestamp(), is("tts"));
    }

    @Test
    public void handleMessage_shouldNotPostIfMessageEditWithSameKeys() {
        when(message.getSlackLink()).thenReturn(link);
        when(message.getTeamId()).thenReturn("T");
        when(message.getChannelId()).thenReturn("C");
        when(message.getUser()).thenReturn("U");
        when(message.getText()).thenReturn("DED-1 a");
        when(message.getPreviousText()).thenReturn("DED-1 b");
        when(message.isMessageEdit()).thenReturn(true);
        when(link.getBotUserId()).thenReturn("B");
        when(slackUser.getUserKey()).thenReturn("JU");

        final ChannelKeyImpl key = new ChannelKeyImpl("JU", "T", "C");
        when(mentionChannelCacheManager.get(key)).thenReturn(Optional.of(new MentionChannel(key, conversation, "")));

        when(slackLinkManager.shouldUseLinkUnfurl("T")).thenReturn(true);
        when(taskBuilder.newUnfurlIssueLinksTask()).thenReturn(unfurlIssueLinksTask);
        when(issueManager.getIssueByCurrentKey("DED-1")).thenReturn(issueDedicated);
        when(dedicatedChannelManager.getDedicatedChannel(issueDedicated)).thenReturn(Optional.of(dedicatedChannel));
        when(slackUserManager.getBySlackUserId("U")).thenReturn(Optional.of(slackUser));
        when(userManager.getUserByKey("JU")).thenReturn(applicationUser);
        when(permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, issueDedicated, applicationUser)).thenReturn(true);
        when(dedicatedChannel.getChannelId()).thenReturn("C2");
        when(taskBuilder.newProcessIssueMentionTask(issueDedicated, message)).thenReturn(processIssueMentionTask);

        boolean result = target.handleMessage(message);

        assertThat(result, is(false));

        verify(taskBuilder).newProcessIssueMentionTask(any(), any());
        verify(taskExecutorService).submitTask(processIssueMentionTask);
        verify(taskBuilder, never()).newSendNotificationTask(any(), (NotificationInfo) any(), any());
        verify(eventPublisher, never()).publish(any());
        verify(issueDetailsMessageManager, never()).sendIssueDetailsMessageToChannel(any(), any(), any());
    }

    @Test
    public void extractIssueKeys_shouldParseKeysFromStringProperly() {
        final IssueReference ref1 = new IssueReference("ISSUE-1", null);

        assertThat(target.extractIssueKeys("ISSUE-1", emptyList()), contains(ref1));
        assertThat(target.extractIssueKeys(" (ISSUE-1) ", emptyList()), contains(ref1));
        assertThat(target.extractIssueKeys(" [ISSUE-1[ ", emptyList()), contains(ref1));
        assertThat(target.extractIssueKeys(" <ISSUE-1> ", emptyList()), contains(ref1));
        assertThat(target.extractIssueKeys(" <ISSUE-1|aadadf> ", emptyList()), contains(ref1));
        assertThat(target.extractIssueKeys(" asd|ISSUE-1|aadadf ", emptyList()), contains(ref1));
        assertThat(target.extractIssueKeys(" aaaa,ISSUE-1,fghfgh ", emptyList()), contains(ref1));
    }

    @Test
    public void extractIssueKeys_shouldParseUrlFromStringProperly() {
        when(applicationProperties.getBaseUrl(UrlMode.CANONICAL)).thenReturn("http://jira.com");

        final IssueReference ref1 = new IssueReference("ISSUE-1", "http://jira.com/browse/ISSUE-1");

        assertThat(target.extractIssueKeys("http://jira.com/browse/ISSUE-1", emptyList()), contains(ref1));
        assertThat(target.extractIssueKeys(" (http://jira.com/browse/ISSUE-1) ", emptyList()), contains(ref1));
        assertThat(target.extractIssueKeys(" [http://jira.com/browse/ISSUE-1[ ", emptyList()), contains(ref1));
        assertThat(target.extractIssueKeys(" <http://jira.com/browse/ISSUE-1> ", emptyList()), contains(ref1));
        assertThat(target.extractIssueKeys(" <http://jira.com/browse/ISSUE-1|aadadf> ", emptyList()), contains(ref1));
        assertThat(target.extractIssueKeys(" asd|http://jira.com/browse/ISSUE-1|aadadf ", emptyList()), contains(ref1));
        assertThat(target.extractIssueKeys(" aaaa,http://jira.com/browse/ISSUE-1,fghfgh ", emptyList()), contains(ref1));
        assertThat(target.extractIssueKeys(" aaaa,<http://jira.com/browse/ISSUE-1|ISSUE-1>,fghfgh ", emptyList()), contains(ref1));

        assertThat(target.extractIssueKeys(" !http://jira.com/browse/ISSUE-1?=1) ", emptyList()),
                contains(new IssueReference("ISSUE-1", "http://jira.com/browse/ISSUE-1")));
    }

    @Test
    public void extractIssueKeys_shouldParseLinksProperly() {
        when(applicationProperties.getBaseUrl(UrlMode.CANONICAL)).thenReturn("http://jira.com");

        final IssueReference ref1 = new IssueReference("ISSUE-1", "http://jira.com/browse/ISSUE-1");
        final IssueReference ref2 = new IssueReference("ISSUE-2", "http://jira.com/browse/ISSUE-2?abc=123");

        assertThat(target.extractIssueKeys("http://jira.com/browse/ISSUE-1",
                singletonList("http://jira.com/browse/ISSUE-2?abc=123")), containsInAnyOrder(ref1, ref2));

        assertThat(target.extractIssueKeys(" a a asdc ", Arrays.asList(
                "http://jira.com/browse/ISSUE-2?abc=123", "http://jira2.com/browse/ISSUE-2")), containsInAnyOrder(ref2));
    }
}
