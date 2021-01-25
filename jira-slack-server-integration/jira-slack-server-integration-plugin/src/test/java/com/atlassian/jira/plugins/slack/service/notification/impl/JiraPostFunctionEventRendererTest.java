package com.atlassian.jira.plugins.slack.service.notification.impl;

import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.priority.Priority;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.plugins.slack.model.SlackNotification;
import com.atlassian.jira.plugins.slack.model.event.JiraPostFunctionEvent;
import com.atlassian.jira.plugins.slack.model.event.PluginEvent;
import com.atlassian.jira.plugins.slack.service.notification.AttachmentHelper;
import com.atlassian.jira.plugins.slack.service.notification.NotificationInfo;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.sal.api.message.I18nResolver;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest.ChatPostMessageRequestBuilder;
import com.github.seratch.jslack.api.model.Attachment;
import io.atlassian.fugue.Either;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class JiraPostFunctionEventRendererTest {
    @Mock
    private I18nResolver i18nResolver;
    @Mock
    private AttachmentHelper attachmentHelper;
    @Mock
    private CustomFieldManager customFieldManager;

    @Mock
    private JiraPostFunctionEvent event;
    @Mock
    private PluginEvent unknownEvent;
    @Mock
    private NotificationInfo notificationInfo;
    @Mock
    private Issue issue;
    @Mock
    private Project project;
    @Mock
    private SlackLink link;
    @Mock
    private Attachment attachment;
    @Mock
    private ApplicationUser applicationUser;
    @Mock
    private ApplicationUser creator;
    @Mock
    private ApplicationUser reporter;
    @Mock
    private ApplicationUser assignee;
    @Mock
    private Status status;
    @Mock
    private Priority priority;
    @Mock
    private IssueType issueType;
    @Mock
    private CustomField customField1;
    @Mock
    private CustomField customField2;
    @Mock
    private CustomField customField3;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private JiraPostFunctionEventRenderer target;

    @Test
    public void doRender_standardNotification() {
        when(event.isHavingErrors()).thenReturn(false);
        when(event.getCustomMessageFormat()).thenReturn(null);
        when(event.getIssue()).thenReturn(issue);
        when(attachmentHelper.buildIssueAttachment(null, issue, null)).thenReturn(attachment);

        SlackNotification notif = testRender();

        assertThat(notif.getMessageRequest().getAttachments(), hasSize(1));
        assertThat(notif.getMessageRequest().getAttachments().get(0), sameInstance(attachment));
    }

    @Test
    public void doRender_errorNotification() {
        when(event.isHavingErrors()).thenReturn(true);
        when(event.getIssue()).thenReturn(issue);
        when(issue.getProjectObject()).thenReturn(project);
        when(project.getKey()).thenReturn("P");
        when(attachmentHelper.projectUrl("P")).thenReturn("purl");
        when(i18nResolver.getText("slack.notification.configerror", "purl")).thenReturn("txt");

        SlackNotification notif = testRender();

        assertThat(notif.getMessageRequest().getAttachments(), hasSize(1));
        assertThat(notif.getMessageRequest().getAttachments().get(0).getText(), is("txt"));
        assertThat(notif.getMessageRequest().getAttachments().get(0).getColor(), is("danger"));
    }

    @Test
    public void doRender_customNotification() {
        when(event.isHavingErrors()).thenReturn(false);
        when(event.getCustomMessageFormat()).thenReturn("$user.displayName");
        when(event.getIssue()).thenReturn(issue);
        when(event.getActor()).thenReturn(applicationUser);
        when(applicationUser.getDisplayName()).thenReturn("UN");
        when(attachmentHelper.buildIssueAttachment(null, issue, null)).thenReturn(attachment);

        SlackNotification notif = testRender();

        assertThat(notif.getMessageRequest().getText(), is("UN"));
        assertThat(notif.getMessageRequest().getAttachments(), hasSize(1));
        assertThat(notif.getMessageRequest().getAttachments().get(0), sameInstance(attachment));
    }

    @Test
    public void doRender_preventsDynamicClassLoading() {
        when(event.isHavingErrors()).thenReturn(false);
        when(event.getCustomMessageFormat()).thenReturn("#set($s='') $s.getClass().forName('java.lang.Runtime').getRuntime().availableProcessors()");
        when(event.getIssue()).thenReturn(issue);
        when(issue.getProjectObject()).thenReturn(project);
        when(project.getKey()).thenReturn("P");
        when(event.getActor()).thenReturn(applicationUser);
        when(applicationUser.getDisplayName()).thenReturn("UN");
        when(attachmentHelper.projectUrl("P")).thenReturn("purl");
        when(attachmentHelper.buildIssueAttachment(null, issue, null)).thenReturn(attachment);

        SlackNotification notif = testRender();

        assertThat(notif.getMessageRequest().getText(), is(" $s.getClass().forName('java.lang.Runtime').getRuntime().availableProcessors()"));
        assertThat(notif.getMessageRequest().getAttachments(), hasSize(1));
        assertThat(notif.getMessageRequest().getAttachments().get(0), sameInstance(attachment));
    }

    private SlackNotification testRender() {
        when(notificationInfo.getLink()).thenReturn(link);
        when(notificationInfo.getChannelId()).thenReturn("C");
        when(notificationInfo.getResponseUrl()).thenReturn("url");
        when(notificationInfo.getThreadTimestamp()).thenReturn("tts");
        when(notificationInfo.getConfigurationOwner()).thenReturn("O");

        List<SlackNotification> result = target.render(event, Collections.singletonList(notificationInfo));

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
        assertThat(target.canRender(event), is(true));
        assertThat(target.canRender(unknownEvent), is(false));
    }

    @Test
    public void renderPreviewNotification_usingDifferentValues() {
        when(event.getCustomMessageFormat()).thenReturn("$user, $issue, $project, $status, $priority, $issueType, "
                + "$creator, $assignee, $reporter, $action, $firstStep, $endStep, $issue.summary, "
                + "$customFields.f1, $customFields.custom_field_2.value, $customFields.f3");
        when(event.getActor()).thenReturn(applicationUser);
        when(applicationUser.getDisplayName()).thenReturn("UN");
        when(event.getIssue()).thenReturn(issue);
        when(issue.getKey()).thenReturn("IK");
        when(issue.getSummary()).thenReturn("Summary");
        when(issue.getProjectObject()).thenReturn(project);
        when(project.getName()).thenReturn("Project");
        when(issue.getStatus()).thenReturn(status);
        when(status.getName()).thenReturn("DONE");
        when(issue.getPriority()).thenReturn(priority);
        when(priority.getName()).thenReturn("H");
        when(issue.getIssueType()).thenReturn(issueType);
        when(issueType.getName()).thenReturn("Task");
        when(issue.getCreator()).thenReturn(creator);
        when(creator.getDisplayName()).thenReturn("CR");
        when(issue.getAssignee()).thenReturn(assignee);
        when(assignee.getDisplayName()).thenReturn("AS");
        when(issue.getReporter()).thenReturn(reporter);
        when(reporter.getDisplayName()).thenReturn("REP");
        when(event.getActionName()).thenReturn("act");
        when(event.getFirstStepName()).thenReturn("TODO");
        when(event.getEndStepName()).thenReturn("INPROG");
        when(customFieldManager.getCustomFieldObjects(issue)).thenReturn(asList(customField1, customField2, customField3));
        when(customField1.getFieldName()).thenReturn("f1");
        when(customField1.getName()).thenReturn("F1");
        when(customField1.getValue(issue)).thenReturn(3L);
        when(customField2.getId()).thenReturn("custom_field_2");
        when(customField2.getValue(issue)).thenReturn("bar");
        when(customField3.getFieldName()).thenReturn("f3");
        when(customField3.getName()).thenReturn("F3");
        when(customField3.getValue(issue)).thenReturn(null);
        when(attachmentHelper.buildIssueAttachment(null, issue, null)).thenReturn(attachment);

        final Either<Throwable, ChatPostMessageRequestBuilder> result = target.renderPreviewNotification(event);

        assertThat(result.isRight(), is(true));

        final ChatPostMessageRequest message = result.right().get().build();
        assertThat(message.getText(), is("UN, IK, Project, DONE, H, Task, "
                + "CR, AS, REP, act, TODO, INPROG, Summary, F1: 3, bar, F3: [empty]"));
        assertThat(message.getAttachments(), hasSize(1));
        assertThat(message.getAttachments().get(0), sameInstance(attachment));
    }
}
