package com.atlassian.jira.plugins.slack.service.notification.impl;

import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.plugins.slack.model.SlackNotification;
import com.atlassian.jira.plugins.slack.model.event.ConfigurationEvent;
import com.atlassian.jira.plugins.slack.model.event.DedicatedChannelLinkedEvent;
import com.atlassian.jira.plugins.slack.model.event.DedicatedChannelUnlinkedEvent;
import com.atlassian.jira.plugins.slack.model.event.PluginEvent;
import com.atlassian.jira.plugins.slack.model.event.ProjectMappingConfigurationEvent;
import com.atlassian.jira.plugins.slack.model.event.UnauthorizedUnfurlEvent;
import com.atlassian.jira.plugins.slack.service.notification.AttachmentHelper;
import com.atlassian.jira.plugins.slack.service.notification.NotificationInfo;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugins.slack.analytics.AnalyticsContext;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.sal.api.message.I18nResolver;
import com.github.seratch.jslack.api.model.Attachment;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.List;

import static com.atlassian.jira.plugins.slack.model.event.ConfigurationEvent.ConfigurationEventType.CHANNEL_LINKED;
import static com.atlassian.jira.plugins.slack.model.event.ConfigurationEvent.ConfigurationEventType.CHANNEL_UNLINKED;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class ConfigurationEventRendererTest {
    @Mock
    private I18nResolver i18nResolver;
    @Mock
    private AttachmentHelper attachmentHelper;
    @Mock
    private IssueManager issueManager;

    @Mock
    private ApplicationUser applicationUser;
    @Mock
    private NotificationInfo notificationInfo;
    @Mock
    private MutableIssue issue;
    @Mock
    private SlackLink link;
    @Mock
    private Attachment attachment;
    @Mock
    private AnalyticsContext context;

    @Mock
    private ConfigurationEvent configurationEvent;
    @Mock
    private PluginEvent unknownEvent;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private ConfigurationEventRenderer target;

    @Test
    public void doRender_visitDedicatedChannelLinkedEvent() {
        when(issue.getKey()).thenReturn("K");
        when(attachmentHelper.issueUrl("K")).thenReturn("iurl");
        when(i18nResolver.getText(
                "jira.plugins.slack.viewissue.panel.dedicated.channel.configured.notification.card.with.autoconvert",
                "iurl|K"
        )).thenReturn("txt");
        when(issueManager.getIssueByKeyIgnoreCase("K")).thenReturn(issue);
        when(attachmentHelper.buildIssueAttachment("txt", issue, null)).thenReturn(attachment);

        SlackNotification notif = testRender(new DedicatedChannelLinkedEvent(context, 5L, "K",  "C", "O"));

        assertThat(notif.getMessageRequest().getAttachments(), hasSize(1));
        assertThat(notif.getMessageRequest().getAttachments().get(0), sameInstance(attachment));
    }

    @Test
    public void doRender_visitDedicatedChannelUnlinkedEvent() {
        when(issue.getKey()).thenReturn("K");
        when(attachmentHelper.issueUrl("K")).thenReturn("iurl");
        when(i18nResolver.getText(
                "jira.plugins.slack.viewissue.panel.dedicated.channel.removed.notification.card",
                "iurl|K"
        )).thenReturn("txt");

        SlackNotification notif = testRender(new DedicatedChannelUnlinkedEvent(context, 5L, "K",  "C", "O"));

        assertThat(notif.getMessageRequest().getText(), is("txt"));
    }

    @Test
    public void doRender_visitProjectMappingConfigurationEventForChannelLinked() {
        when(attachmentHelper.projectUrl("P")).thenReturn("purl");
        when(attachmentHelper.userLink(applicationUser)).thenReturn("uurl");
        when(i18nResolver.getText(
                "jira.plugins.slack.channelmapping.add.notification.card",
                "uurl",
                "purl|Proj"
        )).thenReturn("txt");

        SlackNotification notif = testRender(ProjectMappingConfigurationEvent.builder()
                .eventType(CHANNEL_LINKED)
                .projectId(7L)
                .teamId("T")
                .projectKey("P")
                .projectName("Proj")
                .channelId("C")
                .user(applicationUser)
                .build());

        assertThat(notif.getMessageRequest().getText(), is("txt"));
    }

    @Test
    public void doRender_visitProjectMappingConfigurationEventForChannelUnlinked() {
        when(attachmentHelper.projectUrl("P")).thenReturn("purl");
        when(i18nResolver.getText(
                "jira.plugins.slack.channelmapping.delete.notification.card",
                "purl|Proj"
        )).thenReturn("txt");

        SlackNotification notif = testRender(ProjectMappingConfigurationEvent.builder()
                .eventType(CHANNEL_UNLINKED)
                .projectId(7L)
                .teamId("T")
                .projectKey("P")
                .projectName("Proj")
                .channelId("C")
                .user(applicationUser)
                .build());

        assertThat(notif.getMessageRequest().getText(), is("txt"));
    }

    @Test
    public void doRender_visitUnauthorizedUnfurlConfigurationEvent() {
        when(issue.getKey()).thenReturn("K");
        when(attachmentHelper.jiraUrl()).thenReturn("jurl");
        when(attachmentHelper.issueUrl("K")).thenReturn("iurl");
        when(i18nResolver.getText(
                "jira.slack.unauthenticated.unfurl.message",
                "iurl|K",
                "jurl/plugins/servlet/slack/view-oauth-sessions"
        )).thenReturn("txt");

        SlackNotification notif = testRender(new UnauthorizedUnfurlEvent(context, 5L, "K",  "C", "O"));

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
        assertThat(target.canRender(configurationEvent), is(true));
        assertThat(target.canRender(unknownEvent), is(false));
    }
}
