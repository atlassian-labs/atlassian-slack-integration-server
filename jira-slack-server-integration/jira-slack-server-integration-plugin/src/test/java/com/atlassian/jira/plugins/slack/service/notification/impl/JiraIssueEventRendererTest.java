package com.atlassian.jira.plugins.slack.service.notification.impl;

import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.comments.Comment;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.plugins.slack.model.EventMatcherType;
import com.atlassian.jira.plugins.slack.model.SlackNotification;
import com.atlassian.jira.plugins.slack.model.event.JiraIssueEvent;
import com.atlassian.jira.plugins.slack.model.event.PluginEvent;
import com.atlassian.jira.plugins.slack.service.notification.AttachmentHelper;
import com.atlassian.jira.plugins.slack.service.notification.NotificationInfo;
import com.atlassian.jira.plugins.slack.util.changelog.ChangeLogExtractor;
import com.atlassian.jira.plugins.slack.util.changelog.ChangeLogItem;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.notification.Verbosity;
import com.atlassian.sal.api.message.I18nResolver;
import com.github.seratch.jslack.api.model.Attachment;
import com.github.seratch.jslack.api.model.Field;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class JiraIssueEventRendererTest {
    @Mock
    private I18nResolver i18nResolver;
    @Mock
    private ChangeLogExtractor changeLogExtractor;
    @Mock
    private AttachmentHelper attachmentHelper;
    @Mock
    private UserManager userManager;

    @Mock
    private ChangeLogItem changeLogItem;
    @Mock
    private ApplicationUser applicationUser;
    @Mock
    private ApplicationUser applicationUserNew;
    @Mock
    private ApplicationUser applicationUserOld;
    @Mock
    private NotificationInfo notificationInfo;
    @Mock
    private MutableIssue issue;
    @Mock
    private SlackLink link;
    @Mock
    private Attachment attachment;
    @Mock
    private JiraIssueEvent jiraIssueEvent;
    @Mock
    private PluginEvent unknownEvent;
    @Mock
    private Comment comment;
    @Mock
    private IssueType issueType;
    @Mock
    private Status status;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private JiraIssueEventRenderer target;

    @Test
    public void doRender_visitCreated() {
        when(i18nResolver.getText("jira.slack.card.notification.event.created", "ulink", "a", "Bug", "")).thenReturn("txt");
        when(attachmentHelper.buildIssueAttachment(null, issue, null)).thenReturn(attachment);
        when(jiraIssueEvent.getEventMatcher()).thenReturn(EventMatcherType.ISSUE_CREATED);

        SlackNotification notif = testRender(Verbosity.EXTENDED);

        assertThat(notif.getMessageRequest().getText(), is("txt"));
        assertThat(notif.getMessageRequest().getAttachments(), hasSize(1));
        assertThat(notif.getMessageRequest().getAttachments().get(0), sameInstance(attachment));
    }

    @Test
    public void doRender_visitCreatedWithBasicVerbosity() {
        when(i18nResolver.getText("jira.slack.card.notification.event.created", "ulink", "a", "Bug", "issue-link"))
                .thenReturn("txt");
        when(attachmentHelper.buildIssueAttachment(null, issue, null)).thenReturn(attachment);
        when(attachmentHelper.issueLink(issue)).thenReturn("issue-link");
        when(jiraIssueEvent.getEventMatcher()).thenReturn(EventMatcherType.ISSUE_CREATED);

        SlackNotification notif = testRender(Verbosity.BASIC);

        assertThat(notif.getMessageRequest().getText(), is("txt"));
        assertThat(notif.getMessageRequest().getAttachments(), nullValue());
    }

    @Test
    public void doRender_visitUpdated() {
        Field f = Field.builder()
                .title("F")
                .value("N")
                .valueShortEnough(true)
                .build();
        when(i18nResolver.getText("jira.slack.card.notification.event.updated", "ulink", "a", "Bug", "")).thenReturn("txt");
        when(attachmentHelper.buildIssueAttachment(null, issue, Collections.singletonList(f))).thenReturn(attachment);
        when(jiraIssueEvent.getEventMatcher()).thenReturn(EventMatcherType.ISSUE_UPDATED);
        when(changeLogExtractor.getChanges(ArgumentMatchers.any(), eq(1000))).thenReturn(Collections.singletonList(changeLogItem));
        when(changeLogItem.getNewText()).thenReturn("N");
        when(changeLogItem.getField()).thenReturn("F");

        SlackNotification notif = testRender(Verbosity.EXTENDED);

        assertThat(notif.getMessageRequest().getText(), is("txt"));
        assertThat(notif.getMessageRequest().getAttachments(), hasSize(1));
        assertThat(notif.getMessageRequest().getAttachments().get(0), sameInstance(attachment));
    }

    @Test
    public void doRender_visitTransitioned() {
        when(issue.getStatus()).thenReturn(status);
        when(status.getName()).thenReturn("S");
        when(i18nResolver.getText("jira.slack.card.notification.event.transitioned.with.previous", "ulink", "a", "Bug",
                "", "OLD", "S")).thenReturn("txt");
        when(attachmentHelper.buildIssueAttachment(null, issue, null)).thenReturn(attachment);
        when(jiraIssueEvent.getEventMatcher()).thenReturn(EventMatcherType.ISSUE_TRANSITIONED);
        when(changeLogExtractor.getChanges(ArgumentMatchers.any(), eq(1000))).thenReturn(Collections.singletonList(changeLogItem));
        when(changeLogItem.getOldText()).thenReturn("OLD");
        when(changeLogItem.getNewText()).thenReturn("N");
        when(changeLogItem.getField()).thenReturn(ChangeLogExtractor.STATUS_FIELD_NAME);

        SlackNotification notif = testRender(Verbosity.EXTENDED);

        assertThat(notif.getMessageRequest().getText(), is("txt"));
        assertThat(notif.getMessageRequest().getAttachments(), hasSize(1));
        assertThat(notif.getMessageRequest().getAttachments().get(0), sameInstance(attachment));
    }

    @Test
    public void doRender_visitTransitionedWithoutOldValue() {
        when(issue.getStatus()).thenReturn(status);
        when(status.getName()).thenReturn("S");
        when(i18nResolver.getText("jira.slack.card.notification.event.transitioned", "ulink", "a", "Bug", "", "S"))
                .thenReturn("txt");
        when(attachmentHelper.buildIssueAttachment(null, issue, null)).thenReturn(attachment);
        when(jiraIssueEvent.getEventMatcher()).thenReturn(EventMatcherType.ISSUE_TRANSITIONED);
        when(changeLogExtractor.getChanges(ArgumentMatchers.any(), eq(1000))).thenReturn(Collections.singletonList(changeLogItem));
        when(changeLogItem.getOldText()).thenReturn("");
        when(changeLogItem.getNewText()).thenReturn("N");
        when(changeLogItem.getField()).thenReturn(ChangeLogExtractor.STATUS_FIELD_NAME);

        SlackNotification notif = testRender(Verbosity.EXTENDED);

        assertThat(notif.getMessageRequest().getText(), is("txt"));
        assertThat(notif.getMessageRequest().getAttachments(), hasSize(1));
        assertThat(notif.getMessageRequest().getAttachments().get(0), sameInstance(attachment));
    }

    @Test
    public void doRender_visitCommented() {
        when(i18nResolver.getText("jira.slack.card.notification.event.commented", "ulink", "a", "Bug", ""))
                .thenReturn("txt");
        when(attachmentHelper.buildCommentAttachment(null, issue, comment)).thenReturn(attachment);
        when(comment.getCreated()).thenReturn(new Date());
        when(comment.getUpdated()).thenReturn(null);
        when(jiraIssueEvent.getEventMatcher()).thenReturn(EventMatcherType.ISSUE_COMMENTED);

        SlackNotification notif = testRender(Verbosity.EXTENDED);

        assertThat(notif.getMessageRequest().getText(), is("txt"));
        assertThat(notif.getMessageRequest().getAttachments(), hasSize(1));
        assertThat(notif.getMessageRequest().getAttachments().get(0), sameInstance(attachment));
    }

    @Test
    public void doRender_visitCommentedWithBasicVerbosity() {
        when(i18nResolver.getText("jira.slack.card.notification.event.commented", "ulink", "a", "Bug", "issue-link"))
                .thenReturn("txt");
        when(attachmentHelper.buildCommentAttachment(null, issue, comment)).thenReturn(attachment);
        when(attachmentHelper.issueLink(issue)).thenReturn("issue-link");
        when(comment.getCreated()).thenReturn(new Date());
        when(comment.getUpdated()).thenReturn(null);
        when(comment.getBody()).thenReturn("comment-body");
        when(jiraIssueEvent.getEventMatcher()).thenReturn(EventMatcherType.ISSUE_COMMENTED);

        SlackNotification notif = testRender(Verbosity.BASIC);

        assertThat(notif.getMessageRequest().getText(), is("txt: _comment-body_"));
        assertThat(notif.getMessageRequest().getAttachments(), nullValue());
    }

    @Test
    public void doRender_visitCommentedWithUpdate() {
        when(i18nResolver.getText("jira.slack.card.notification.event.comment.updated", "ulink", "a", "Bug", ""))
                .thenReturn("txt");
        when(attachmentHelper.buildCommentAttachment(null, issue, comment)).thenReturn(attachment);
        when(comment.getCreated()).thenReturn(new Date(1L));
        when(comment.getUpdated()).thenReturn(new Date(2L));
        when(jiraIssueEvent.getEventMatcher()).thenReturn(EventMatcherType.ISSUE_COMMENTED);

        SlackNotification notif = testRender(Verbosity.EXTENDED);

        assertThat(notif.getMessageRequest().getText(), is("txt"));
        assertThat(notif.getMessageRequest().getAttachments(), hasSize(1));
        assertThat(notif.getMessageRequest().getAttachments().get(0), sameInstance(attachment));
    }

    @Test
    public void doRender_visitAssignmentChangedWithBothOldAndNew() {
        when(i18nResolver.getText("jira.slack.card.notification.event.assigned.from", "ulink", "a", "Bug", "",
                "olink", "nlink")).thenReturn("txt");
        when(attachmentHelper.buildIssueAttachment(null, issue, null)).thenReturn(attachment);
        when(jiraIssueEvent.getEventMatcher()).thenReturn(EventMatcherType.ISSUE_ASSIGNMENT_CHANGED);
        when(changeLogExtractor.getChanges(ArgumentMatchers.any(), eq(1000))).thenReturn(Collections.singletonList(changeLogItem));
        when(changeLogItem.getField()).thenReturn(ChangeLogExtractor.ASSIGNEE_FIELD_NAME);
        when(changeLogItem.getOldValue()).thenReturn("OLDV");
        when(changeLogItem.getNewValue()).thenReturn("NEWV");
        when(changeLogItem.getNewText()).thenReturn("NEW");
        when(userManager.getUserByKey("OLDV")).thenReturn(applicationUserOld);
        when(userManager.getUserByKey("NEWV")).thenReturn(applicationUserNew);
        when(attachmentHelper.userLink(applicationUserOld)).thenReturn("olink");
        when(attachmentHelper.userLink(applicationUserNew)).thenReturn("nlink");

        SlackNotification notif = testRender(Verbosity.EXTENDED);

        assertThat(notif.getMessageRequest().getText(), is("txt"));
        assertThat(notif.getMessageRequest().getAttachments(), hasSize(1));
        assertThat(notif.getMessageRequest().getAttachments().get(0), sameInstance(attachment));
    }

    @Test
    public void doRender_visitAssignmentChangedWithNewUserOnly() {
        when(i18nResolver.getText("jira.slack.card.notification.event.assigned", "ulink", "a", "Bug", "", "nlink"))
                .thenReturn("txt");
        when(attachmentHelper.buildIssueAttachment(null, issue, null)).thenReturn(attachment);
        when(jiraIssueEvent.getEventMatcher()).thenReturn(EventMatcherType.ISSUE_ASSIGNMENT_CHANGED);
        when(changeLogExtractor.getChanges(ArgumentMatchers.any(), eq(1000))).thenReturn(Collections.singletonList(changeLogItem));
        when(changeLogItem.getField()).thenReturn(ChangeLogExtractor.ASSIGNEE_FIELD_NAME);
        when(changeLogItem.getOldValue()).thenReturn("");
        when(changeLogItem.getNewValue()).thenReturn("NEWV");
        when(changeLogItem.getNewText()).thenReturn("NEW");
        when(userManager.getUserByKey("NEWV")).thenReturn(applicationUserNew);
        when(attachmentHelper.userLink(applicationUserNew)).thenReturn("nlink");

        SlackNotification notif = testRender(Verbosity.EXTENDED);

        assertThat(notif.getMessageRequest().getText(), is("txt"));
        assertThat(notif.getMessageRequest().getAttachments(), hasSize(1));
        assertThat(notif.getMessageRequest().getAttachments().get(0), sameInstance(attachment));
    }

    @Test
    public void doRender_visitAssignmentChangedWithOldUserOnly() {
        when(i18nResolver.getText("jira.slack.card.notification.event.unassigned.from", "ulink", "a", "Bug", "","olink"))
                .thenReturn("txt");
        when(attachmentHelper.buildIssueAttachment(null, issue, null)).thenReturn(attachment);
        when(jiraIssueEvent.getEventMatcher()).thenReturn(EventMatcherType.ISSUE_ASSIGNMENT_CHANGED);
        when(changeLogExtractor.getChanges(ArgumentMatchers.any(), eq(1000))).thenReturn(Collections.singletonList(changeLogItem));
        when(changeLogItem.getField()).thenReturn(ChangeLogExtractor.ASSIGNEE_FIELD_NAME);
        when(changeLogItem.getOldValue()).thenReturn("OLDV");
        when(changeLogItem.getNewValue()).thenReturn(null);
        when(changeLogItem.getNewText()).thenReturn("Unassiged");
        when(attachmentHelper.userLink(applicationUserOld)).thenReturn("olink");
        when(userManager.getUserByKey("OLDV")).thenReturn(applicationUserOld);

        SlackNotification notif = testRender(Verbosity.EXTENDED);

        assertThat(notif.getMessageRequest().getText(), is("txt"));
        assertThat(notif.getMessageRequest().getAttachments(), hasSize(1));
        assertThat(notif.getMessageRequest().getAttachments().get(0), sameInstance(attachment));
    }

    @Test
    public void doRender_visitAssignmentChangedWithoutOldUserOrNewUser() {
        when(i18nResolver.getText("jira.slack.card.notification.event.unassigned", "ulink", "a", "Bug", ""))
                .thenReturn("txt");
        when(attachmentHelper.buildIssueAttachment(null, issue, null)).thenReturn(attachment);
        when(jiraIssueEvent.getEventMatcher()).thenReturn(EventMatcherType.ISSUE_ASSIGNMENT_CHANGED);
        when(changeLogExtractor.getChanges(ArgumentMatchers.any(), eq(1000))).thenReturn(Collections.singletonList(changeLogItem));
        when(changeLogItem.getField()).thenReturn(ChangeLogExtractor.ASSIGNEE_FIELD_NAME);
        when(changeLogItem.getOldValue()).thenReturn(null);
        when(changeLogItem.getNewValue()).thenReturn(null);
        when(changeLogItem.getNewText()).thenReturn("Unassiged");

        SlackNotification notif = testRender(Verbosity.EXTENDED);

        assertThat(notif.getMessageRequest().getText(), is("txt"));
        assertThat(notif.getMessageRequest().getAttachments(), hasSize(1));
        assertThat(notif.getMessageRequest().getAttachments().get(0), sameInstance(attachment));
    }

    private SlackNotification testRender(final Verbosity verbosity) {
        IssueEvent issueEvent = new IssueEvent(issue, applicationUser, comment, null, null, Collections.emptyMap(), 1L);
        when(jiraIssueEvent.getIssue()).thenReturn(issue);
        when(notificationInfo.getLink()).thenReturn(link);
        when(notificationInfo.getChannelId()).thenReturn("C");
        when(notificationInfo.getResponseUrl()).thenReturn("url");
        when(notificationInfo.getThreadTimestamp()).thenReturn("tts");
        when(notificationInfo.getConfigurationOwner()).thenReturn("O");
        when(notificationInfo.getVerbosity()).thenReturn(verbosity);
        when(attachmentHelper.userLink(applicationUser)).thenReturn("ulink");
        when(issue.getIssueType()).thenReturn(issueType);
        when(issueType.getName()).thenReturn("Bug");
        when(i18nResolver.getText("jira.slack.card.notification.issue.type.article.a")).thenReturn("a");

        List<SlackNotification> result = target.render(jiraIssueEvent, Collections.singletonList(notificationInfo));

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
        assertThat(target.canRender(jiraIssueEvent), is(true));
        assertThat(target.canRender(unknownEvent), is(false));
    }
}
