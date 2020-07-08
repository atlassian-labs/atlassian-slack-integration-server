package com.atlassian.jira.plugins.slack.service.listener;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.plugins.slack.model.SlackDeletedMessage;
import com.atlassian.jira.plugins.slack.model.SlackIncomingMessage;
import com.atlassian.jira.plugins.slack.model.event.ShowAccountInfoEvent;
import com.atlassian.jira.plugins.slack.model.event.ShowHelpEvent;
import com.atlassian.jira.plugins.slack.model.event.ShowIssueNotFoundEvent;
import com.atlassian.jira.plugins.slack.service.notification.NotificationInfo;
import com.atlassian.jira.plugins.slack.service.task.TaskBuilder;
import com.atlassian.jira.plugins.slack.service.task.TaskExecutorService;
import com.atlassian.jira.plugins.slack.service.task.impl.ProcessMessageDeletedTask;
import com.atlassian.jira.plugins.slack.service.task.impl.SendNotificationTask;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.webhooks.GenericMessageSlackEvent;
import com.atlassian.plugins.slack.api.webhooks.LinkSharedSlackEvent;
import com.atlassian.plugins.slack.api.webhooks.SlackEvent;
import com.atlassian.plugins.slack.api.webhooks.SlackSlashCommand;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SlackEventListenerTest {
    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private TaskExecutorService taskExecutorService;
    @Mock
    private TaskBuilder taskBuilder;
    @Mock
    private SlackLinkManager slackLinkManager;
    @Mock
    private SlackEventHandlerService slackEventHandlerService;

    @Mock
    private SlackSlashCommand command;
    @Mock
    private GenericMessageSlackEvent messageSlackEvent;
    @Mock
    private LinkSharedSlackEvent linkSharedSlackEvent;
    @Mock
    private SendNotificationTask sendNotificationTask;
    @Mock
    private SlackLink link;
    @Mock
    private LinkSharedSlackEvent.Link link1;
    @Mock
    private LinkSharedSlackEvent.Link link2;
    @Mock
    private SlackEvent slackEvent;
    @Mock
    private ProcessMessageDeletedTask processMessageDeletedTask;
    @Mock
    private GenericMessageSlackEvent.ChangedMessage currentMessage;
    @Mock
    private GenericMessageSlackEvent.ChangedMessage changedMessage;

    @Captor
    private ArgumentCaptor<ShowAccountInfoEvent> showAccountInfoEventArgumentCaptor;
    @Captor
    private ArgumentCaptor<SlackIncomingMessage> slackIncomingMessageArgumentCaptor;
    @Captor
    private ArgumentCaptor<NotificationInfo> notificationInfoArgumentCaptor;
    @Captor
    private ArgumentCaptor<SlackDeletedMessage> slackDeletedMessageArgumentCaptor;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private SlackEventListener target;

    @Test
    public void slashCommand_shouldShowHelp() {
        when(command.getText()).thenReturn(" help ");
        when(taskBuilder.newSendNotificationTask(
                isA(ShowHelpEvent.class),
                notificationInfoArgumentCaptor.capture(),
                same(taskExecutorService))).thenReturn(sendNotificationTask);

        testCommand();
    }

    @Test
    public void slashCommand_shouldShowHelpWithEmptyMessage() {
        when(command.getText()).thenReturn("");
        when(taskBuilder.newSendNotificationTask(
                isA(ShowHelpEvent.class),
                notificationInfoArgumentCaptor.capture(),
                same(taskExecutorService))).thenReturn(sendNotificationTask);

        testCommand();
    }

    @Test
    public void slashCommand_shouldShowAccountInfo() {
        when(command.getText()).thenReturn(" account ");
        when(command.getUserId()).thenReturn("U");
        when(taskBuilder.newSendNotificationTask(
                showAccountInfoEventArgumentCaptor.capture(),
                notificationInfoArgumentCaptor.capture(),
                same(taskExecutorService))).thenReturn(sendNotificationTask);

        testCommand();

        ShowAccountInfoEvent event = showAccountInfoEventArgumentCaptor.getValue();
        assertThat(event.getSlackUserId(), is("U"));
    }

    @Test
    public void slashCommand_shouldShowIssueNotFound() {
        when(command.getText()).thenReturn(" abc ");
        when(command.getTeamId()).thenReturn("T");
        when(command.getUserId()).thenReturn("U");
        when(slackEventHandlerService.handleMessage(slackIncomingMessageArgumentCaptor.capture())).thenReturn(false);
        when(taskBuilder.newSendNotificationTask(
                isA(ShowIssueNotFoundEvent.class),
                notificationInfoArgumentCaptor.capture(),
                same(taskExecutorService))).thenReturn(sendNotificationTask);

        testCommand();

        SlackIncomingMessage msg = slackIncomingMessageArgumentCaptor.getValue();
        assertThat(msg.getTeamId(), is("T"));
        assertThat(msg.getSlackLink(), sameInstance(link));
        assertThat(msg.getChannelId(), is("C"));
        assertThat(msg.getText(), is("abc"));
        assertThat(msg.getUser(), is("U"));
        assertThat(msg.getResponseUrl(), is("url"));
        assertThat(msg.isMessageEdit(), is(false));
        assertThat(msg.isLinkShared(), is(false));
        assertThat(msg.isSlashCommand(), is(true));
    }

    @Test
    public void slashCommand_shouldUnfurlIssue() {
        when(command.getText()).thenReturn(" abc ");
        when(command.getTeamId()).thenReturn("T");
        when(command.getUserId()).thenReturn("U");
        when(slackEventHandlerService.handleMessage(any())).thenReturn(true);
        when(command.getChannelId()).thenReturn("C");
        when(command.getResponseUrl()).thenReturn("url");
        when(command.getSlackLink()).thenReturn(link);

        target.slashCommand(command);

        verify(taskExecutorService, never()).submitTask(any());
    }

    private void testCommand() {
        when(command.getChannelId()).thenReturn("C");
        when(command.getResponseUrl()).thenReturn("url");
        when(command.getSlackLink()).thenReturn(link);

        target.slashCommand(command);

        verify(taskExecutorService).submitTask(sendNotificationTask);

        NotificationInfo notifInfo = notificationInfoArgumentCaptor.getValue();
        assertThat(notifInfo.getLink(), sameInstance(link));
        assertThat(notifInfo.getChannelId(), is("C"));
        assertThat(notifInfo.getResponseUrl(), is("url"));
    }

    @Test
    public void messageReceived_shouldSubmitMessageDeletion() {
        when(messageSlackEvent.isDeletedEvent()).thenReturn(true);
        when(taskBuilder.newProcessMessageDeletionTask(slackDeletedMessageArgumentCaptor.capture()))
                .thenReturn(processMessageDeletedTask);
        when(messageSlackEvent.getPreviousMessage()).thenReturn(changedMessage);
        when(changedMessage.getTs()).thenReturn("cts");
        when(messageSlackEvent.getSlackEvent()).thenReturn(slackEvent);
        when(messageSlackEvent.getChannel()).thenReturn("C");
        when(slackEvent.getTeamId()).thenReturn("T");
        when(slackEvent.getSlackLink()).thenReturn(link);

        target.messageReceived(messageSlackEvent);

        verify(taskExecutorService).submitTask(processMessageDeletedTask);

        SlackDeletedMessage msg = slackDeletedMessageArgumentCaptor.getValue();
        assertThat(msg.getTeamId(), is("T"));
        assertThat(msg.getChannelId(), is("C"));
        assertThat(msg.getTs(), is("cts"));
        assertThat(msg.getSlackLink(), sameInstance(link));
    }

    @Test
    public void messageReceived_shouldSubmitChangedEvent() {
        when(messageSlackEvent.isChangedEvent()).thenReturn(true);
        when(messageSlackEvent.getPreviousMessage()).thenReturn(changedMessage);
        when(messageSlackEvent.getMessage()).thenReturn(currentMessage);
        when(changedMessage.getText()).thenReturn("OT");
        when(currentMessage.getText()).thenReturn("NT");
        when(currentMessage.getTs()).thenReturn("ts");
        when(currentMessage.getUser()).thenReturn("U");
        when(messageSlackEvent.getSlackEvent()).thenReturn(slackEvent);
        when(messageSlackEvent.getChannel()).thenReturn("C");
        when(messageSlackEvent.getThreadTimestamp()).thenReturn("tts");
        when(slackEvent.getTeamId()).thenReturn("T");
        when(slackEvent.getSlackLink()).thenReturn(link);

        target.messageReceived(messageSlackEvent);

        verify(slackEventHandlerService).handleMessage(slackIncomingMessageArgumentCaptor.capture());

        SlackIncomingMessage msg = slackIncomingMessageArgumentCaptor.getValue();
        assertThat(msg.getTeamId(), is("T"));
        assertThat(msg.getSlackLink(), sameInstance(link));
        assertThat(msg.getChannelId(), is("C"));
        assertThat(msg.getText(), is("NT"));
        assertThat(msg.getPreviousText(), is("OT"));
        assertThat(msg.getUser(), is("U"));
        assertThat(msg.getTs(), is("ts"));
        assertThat(msg.getThreadTs(), is("tts"));
        assertThat(msg.getResponseUrl(), nullValue());
        assertThat(msg.isMessageEdit(), is(true));
        assertThat(msg.isLinkShared(), is(false));
        assertThat(msg.isSlashCommand(), is(false));
    }

    @Test
    public void messageReceived_unfurlIsHandled() {
        when(slackEventHandlerService.handleMessage(slackIncomingMessageArgumentCaptor.capture())).thenReturn(true);
        when(messageSlackEvent.getSlackEvent()).thenReturn(slackEvent);
        when(messageSlackEvent.getChannel()).thenReturn("C");
        when(messageSlackEvent.getText()).thenReturn("txt");
        when(messageSlackEvent.getTs()).thenReturn("ts");
        when(messageSlackEvent.getThreadTimestamp()).thenReturn("tts");
        when(messageSlackEvent.getUser()).thenReturn("U");
        when(slackEvent.getTeamId()).thenReturn("T");
        when(slackEvent.getSlackLink()).thenReturn(link);

        target.messageReceived(messageSlackEvent);

        SlackIncomingMessage msg = slackIncomingMessageArgumentCaptor.getValue();
        assertThat(msg.getTeamId(), is("T"));
        assertThat(msg.getSlackLink(), sameInstance(link));
        assertThat(msg.getChannelId(), is("C"));
        assertThat(msg.getText(), is("txt"));
        assertThat(msg.getUser(), is("U"));
        assertThat(msg.getTs(), is("ts"));
        assertThat(msg.getThreadTs(), is("tts"));
        assertThat(msg.getResponseUrl(), nullValue());
        assertThat(msg.isMessageEdit(), is(false));
        assertThat(msg.isLinkShared(), is(false));
        assertThat(msg.isSlashCommand(), is(false));
    }

    @Test
    public void messageReceived_showsHelpForDirectMessage() {
        when(slackEventHandlerService.handleMessage(any())).thenReturn(false);
        when(messageSlackEvent.getSlackEvent()).thenReturn(slackEvent);
        when(messageSlackEvent.getChannel()).thenReturn("C");
        when(messageSlackEvent.getText()).thenReturn("help");
        when(messageSlackEvent.getTs()).thenReturn("ts");
        when(messageSlackEvent.getUser()).thenReturn("U");
        when(messageSlackEvent.getChannelType()).thenReturn("im");
        when(slackEvent.getTeamId()).thenReturn("T");
        when(slackEvent.getSlackLink()).thenReturn(link);
        when(taskBuilder.newSendNotificationTask(
                isA(ShowHelpEvent.class),
                notificationInfoArgumentCaptor.capture(),
                same(taskExecutorService))).thenReturn(sendNotificationTask);

        target.messageReceived(messageSlackEvent);

        NotificationInfo notifInfo = notificationInfoArgumentCaptor.getValue();
        assertThat(notifInfo.getLink(), sameInstance(link));
        assertThat(notifInfo.getChannelId(), is("C"));
        assertThat(notifInfo.getResponseUrl(), nullValue());
    }

    @Test
    public void messageReceived_doNothingIfNotSupportedMessageType() {
        when(messageSlackEvent.getSubtype()).thenReturn("S");
        when(messageSlackEvent.getSlackEvent()).thenReturn(slackEvent);

        target.messageReceived(messageSlackEvent);

        verify(slackEventHandlerService, never()).handleMessage(any());
        verify(taskBuilder, never()).newProcessMessageDeletionTask(any());
    }

    @Test
    public void messageReceived_doNothingIfMessageIsHidden() {
        when(messageSlackEvent.getSubtype()).thenReturn("me_message");
        when(messageSlackEvent.isHidden()).thenReturn(true);
        when(messageSlackEvent.getSlackEvent()).thenReturn(slackEvent);

        target.messageReceived(messageSlackEvent);

        verify(slackEventHandlerService, never()).handleMessage(any());
        verify(taskBuilder, never()).newProcessMessageDeletionTask(any());
    }

    @Test
    public void linkShared() {
        when(linkSharedSlackEvent.getSlackEvent()).thenReturn(slackEvent);
        when(linkSharedSlackEvent.getChannel()).thenReturn("C");
        when(linkSharedSlackEvent.getLinks()).thenReturn(Arrays.asList(link1, link2));
        when(linkSharedSlackEvent.getMessageTimestamp()).thenReturn("ts");
        when(linkSharedSlackEvent.getThreadTimestamp()).thenReturn("tts");
        when(linkSharedSlackEvent.getUser()).thenReturn("U");
        when(slackEvent.getTeamId()).thenReturn("T");
        when(slackEvent.getSlackLink()).thenReturn(link);
        when(link1.getUrl()).thenReturn("l1");
        when(link2.getUrl()).thenReturn("l2");
        when(slackLinkManager.shouldUseLinkUnfurl("T")).thenReturn(true);

        target.linkShared(linkSharedSlackEvent);

        verify(slackEventHandlerService).handleMessage(slackIncomingMessageArgumentCaptor.capture());

        SlackIncomingMessage msg = slackIncomingMessageArgumentCaptor.getValue();
        assertThat(msg.getTeamId(), is("T"));
        assertThat(msg.getSlackLink(), sameInstance(link));
        assertThat(msg.getChannelId(), is("C"));
        assertThat(msg.getText(), is(""));
        assertThat(msg.getUser(), is("U"));
        assertThat(msg.getTs(), is("ts"));
        assertThat(msg.getThreadTs(), is("tts"));
        assertThat(msg.getResponseUrl(), nullValue());
        assertThat(msg.isMessageEdit(), is(false));
        assertThat(msg.isLinkShared(), is(true));
        assertThat(msg.isSlashCommand(), is(false));
        assertThat(msg.getLinks(), containsInAnyOrder("l1", "l2"));
    }

    @Test
    public void linkShared_shouldNotUnfurl() {
        when(linkSharedSlackEvent.getSlackEvent()).thenReturn(slackEvent);
        when(slackEvent.getTeamId()).thenReturn("T");

        when(slackLinkManager.shouldUseLinkUnfurl("T")).thenReturn(false);

        target.linkShared(linkSharedSlackEvent);

        verify(slackEventHandlerService, never()).handleMessage(any());
    }
}
