package com.atlassian.jira.plugins.slack.service.notification.impl;

import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.plugins.slack.model.DedicatedChannel;
import com.atlassian.jira.plugins.slack.model.SlackIncomingMessage;
import com.atlassian.jira.plugins.slack.model.SlackNotification;
import com.atlassian.jira.plugins.slack.model.event.IssueMentionedEvent;
import com.atlassian.jira.plugins.slack.model.event.JiraCommandEvent;
import com.atlassian.jira.plugins.slack.model.event.PluginEvent;
import com.atlassian.jira.plugins.slack.model.event.ShowAccountInfoEvent;
import com.atlassian.jira.plugins.slack.model.event.ShowBotAddedHelpEvent;
import com.atlassian.jira.plugins.slack.model.event.ShowHelpEvent;
import com.atlassian.jira.plugins.slack.model.event.ShowIssueEvent;
import com.atlassian.jira.plugins.slack.model.event.ShowIssueNotFoundEvent;
import com.atlassian.jira.plugins.slack.model.event.ShowWelcomeEvent;
import com.atlassian.jira.plugins.slack.service.notification.AttachmentHelper;
import com.atlassian.jira.plugins.slack.service.notification.NotificationInfo;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.SlackUser;
import com.atlassian.plugins.slack.user.SlackUserManager;
import com.atlassian.sal.api.message.I18nResolver;
import com.github.seratch.jslack.api.model.Attachment;
import com.github.seratch.jslack.api.model.Field;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class CommandEventRendererTest {
    @Mock
    private AttachmentHelper attachmentHelper;
    @Mock
    private IssueManager issueManager;
    @Mock
    private I18nResolver i18nResolver;
    @Mock
    private SlackUserManager slackUserManager;
    @Mock
    private UserManager userManager;

    @Mock
    private SlackIncomingMessage slackIncomingMessage;
    @Mock
    private ApplicationUser applicationUser;
    @Mock
    private NotificationInfo notificationInfo;
    @Mock
    private MutableIssue issue;
    @Mock
    private SlackLink link;
    @Mock
    private JiraCommandEvent jiraCommandEvent;
    @Mock
    private PluginEvent unknownEvent;
    @Mock
    private Attachment attachment;
    @Mock
    private DedicatedChannel dedicatedChannel;
    @Mock
    private SlackUser slackUser;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private CommandEventRenderer target;

    @Test
    public void doRender_issueMentionedInSameWorkspace() {
        when(issueManager.getIssueObject(3L)).thenReturn(issue);
        when(slackIncomingMessage.getChannelId()).thenReturn("C");
        when(slackIncomingMessage.getTeamId()).thenReturn("T");
        when(slackIncomingMessage.getText()).thenReturn("slack-text");
        when(i18nResolver.getText("jira.plugins.slack.dedicatedchannel.mention.attachment", "@U", "#C")).thenReturn("txt");
        when(slackIncomingMessage.getUser()).thenReturn("U");
        when(link.getTeamId()).thenReturn("T");

        SlackNotification notif = testRender(new IssueMentionedEvent(slackIncomingMessage, 3L));

        assertThat(notif.getMessageRequest().getText(), is("txt"));
        assertThat(notif.getMessageRequest().getAttachments(), hasSize(1));
        assertThat(notif.getMessageRequest().getAttachments().get(0).getText(), is("slack-text"));
    }

    @Test
    public void doRender_issueMentionedInOtherWorkspace() {
        when(issueManager.getIssueObject(3L)).thenReturn(issue);
        when(slackIncomingMessage.getChannelId()).thenReturn("C");
        when(slackIncomingMessage.getTeamId()).thenReturn("T");
        when(slackIncomingMessage.getText()).thenReturn("slack-text");
        when(i18nResolver.getText("jira.plugins.slack.dedicatedchannel.mention.attachment.external",
                "https://slack.com/app_redirect?team=T&channel=C")).thenReturn("txt");
        when(link.getTeamId()).thenReturn("T2");

        SlackNotification notif = testRender(new IssueMentionedEvent(slackIncomingMessage, 3L));

        assertThat(notif.getMessageRequest().getText(), is("txt"));
        assertThat(notif.getMessageRequest().getAttachments(), hasSize(1));
        assertThat(notif.getMessageRequest().getAttachments().get(0).getText(), is("slack-text"));
    }

    @Test
    public void doRender_visitShowIssueEventWithoutDedicatedChannel() {
        when(attachmentHelper.buildIssueAttachment(null, issue, Collections.emptyList())).thenReturn(attachment);

        SlackNotification notif = testRender(new ShowIssueEvent(issue, null));

        assertThat(notif.getMessageRequest().getAttachments().get(0), sameInstance(attachment));
    }

    @Test
    public void doRender_visitShowIssueEventWithDedicatedChannelInSameWorkspace() {
        when(i18nResolver.getText("jira.plugins.slack.viewissue.panel.dedicated.channel.label")).thenReturn("lab");
        when(link.getTeamId()).thenReturn("T");
        when(attachmentHelper.buildIssueAttachment(null, issue, Collections.singletonList(Field.builder()
                .title("lab")
                .value("<#C>")
                .build()))).thenReturn(attachment);
        when(dedicatedChannel.getTeamId()).thenReturn("T");
        when(dedicatedChannel.getChannelId()).thenReturn("C");
        when(dedicatedChannel.isPrivateChannel()).thenReturn(false);

        SlackNotification notif = testRender(new ShowIssueEvent(issue, dedicatedChannel));

        assertThat(notif.getMessageRequest().getAttachments().get(0), sameInstance(attachment));
    }

    @Test
    public void doRender_visitShowIssueEventWithDedicatedChannelInDifferentWorkspace() {
        when(i18nResolver.getText("jira.plugins.slack.viewissue.panel.dedicated.channel.label")).thenReturn("lab");
        when(link.getTeamId()).thenReturn("T");
        String channelLink = "<https://slack.com/app_redirect?team=T2&channel=C|CN>";
        when(attachmentHelper.buildIssueAttachment(null, issue, Collections.singletonList(Field.builder()
                .title("lab")
                .value(channelLink)
                .build()))).thenReturn(attachment);
        when(dedicatedChannel.getTeamId()).thenReturn("T2");
        when(dedicatedChannel.getChannelId()).thenReturn("C");
        when(dedicatedChannel.getName()).thenReturn("CN");
        when(dedicatedChannel.isPrivateChannel()).thenReturn(false);

        SlackNotification notif = testRender(new ShowIssueEvent(issue, dedicatedChannel));

        assertThat(notif.getMessageRequest().getAttachments().get(0), sameInstance(attachment));
    }

    @Test
    public void doRender_visitShowIssueEventWithDedicatedChannelIsPrivate() {
        when(i18nResolver.getText("jira.plugins.slack.viewissue.panel.dedicated.channel.label")).thenReturn("lab");
        when(i18nResolver.getText("jira.slack.card.notification.private.channel.name")).thenReturn("UN");
        String channelLink = "<https://slack.com/app_redirect?team=T&channel=C|UN>";
        when(attachmentHelper.buildIssueAttachment(null, issue, Collections.singletonList(Field.builder()
                .title("lab")
                .value(channelLink)
                .build()))).thenReturn(attachment);
        when(dedicatedChannel.getTeamId()).thenReturn("T");
        when(dedicatedChannel.getChannelId()).thenReturn("C");
        when(dedicatedChannel.isPrivateChannel()).thenReturn(true);

        SlackNotification notif = testRender(new ShowIssueEvent(issue, dedicatedChannel));

        assertThat(notif.getMessageRequest().getAttachments().get(0), sameInstance(attachment));
    }

    @Test
    public void doRender_visitShowIssueNotFoundEvent() {
        when(i18nResolver.getText("jira.slack.card.notification.no.issues.found")).thenReturn("txt");

        SlackNotification notif = testRender(new ShowIssueNotFoundEvent());

        assertThat(notif.getMessageRequest().getText(), is("txt"));
    }

    @Test
    public void doRender_visitShowHelpEvent() {
        when(attachmentHelper.jiraUrl()).thenReturn("url");
        when(attachmentHelper.jiraTitle()).thenReturn("title");
        when(i18nResolver.getText("jira.slack.card.notification.show.help", "<@bot>", "url|title", "/cmd")).thenReturn("txt");

        SlackNotification notif = testRender(new ShowHelpEvent("bot", "/cmd"));

        assertThat(notif.getMessageRequest().getText(), is("txt"));
    }

    @Test
    public void doRender_visitShowWelcomeEvent() {
        when(attachmentHelper.jiraUrl()).thenReturn("url?atlLinkOrigin=123");
        when(attachmentHelper.jiraTitle()).thenReturn("title");
        when(i18nResolver.getText(
                "jira.slack.card.notification.workspace.connected.welcome",
                "url?atlLinkOrigin=123|title",
                "url/plugins/servlet/slack/configure?teamId=T"
        )).thenReturn("txt");

        SlackNotification notif = testRender(new ShowWelcomeEvent("T"));

        assertThat(notif.getMessageRequest().getText(), is(":tada: txt"));
    }

    @Test
    public void doRender_visitShowBotAddedHelpEvent() {
        when(attachmentHelper.jiraUrl()).thenReturn("url");
        when(attachmentHelper.jiraTitle()).thenReturn("title");
        when(i18nResolver.getText("jira.slack.card.notification.bot.added.to.channel.help", "url|title")).thenReturn("txt");

        SlackNotification notif = testRender(new ShowBotAddedHelpEvent(link, "C"));

        assertThat(notif.getMessageRequest().getText(), is("txt"));
    }

    @Test
    public void doRender_visitShowAccountInfoEvent() {
        when(slackUserManager.getBySlackUserId("U")).thenReturn(Optional.of(slackUser));
        when(slackUser.getUserKey()).thenReturn("K");
        when(userManager.getUserByKey("K")).thenReturn(applicationUser);
        when(attachmentHelper.getAccountMessage(applicationUser)).thenReturn("txt");

        SlackNotification notif = testRender(new ShowAccountInfoEvent("U"));

        assertThat(notif.getMessageRequest().getText(), is("txt"));
    }

    private SlackNotification testRender(PluginEvent pluginEvent) {
        when(notificationInfo.getLink()).thenReturn(link);
        when(notificationInfo.getChannelId()).thenReturn("C");
        when(notificationInfo.getResponseUrl()).thenReturn("url");
        when(notificationInfo.getThreadTimestamp()).thenReturn("tts");
        when(notificationInfo.getConfigurationOwner()).thenReturn("O");

        List<SlackNotification> result = target.render(pluginEvent, Collections.singletonList(notificationInfo));

        assertThat(result, hasSize(1));

        SlackNotification notif = result.get(0);
        assertThat(notif.getSlackLink(), sameInstance(link));
        assertThat(notif.getChannelId(), sameInstance("C"));
        assertThat(notif.getResponseUrl(), is("url"));
        assertThat(notif.getConfigurationOwner(), is("O"));
        assertThat(notif.getMessageRequest().isMrkdwn(), is(true));
        assertThat(notif.getMessageRequest().getChannel(), is("C"));
        assertThat(notif.getMessageRequest().getThreadTs(), is("tts"));

        return notif;
    }

    @Test
    public void canRender() {
        assertThat(target.canRender(jiraCommandEvent), is(true));
        assertThat(target.canRender(unknownEvent), is(false));
    }
}
