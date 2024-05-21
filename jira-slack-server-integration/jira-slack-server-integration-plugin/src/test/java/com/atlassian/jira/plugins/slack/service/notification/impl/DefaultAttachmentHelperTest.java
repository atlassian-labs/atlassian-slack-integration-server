package com.atlassian.jira.plugins.slack.service.notification.impl;

import com.atlassian.jira.avatar.Avatar;
import com.atlassian.jira.avatar.AvatarService;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.comments.Comment;
import com.atlassian.jira.issue.priority.Priority;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.plugins.slack.settings.SlackSettingService;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.UrlMode;
import com.atlassian.sal.api.message.I18nResolver;
import com.github.seratch.jslack.api.model.Attachment;
import com.github.seratch.jslack.api.model.Field;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.net.URI;
import java.util.Collections;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class DefaultAttachmentHelperTest {
    @Mock
    private ApplicationProperties salApplicationProperties;
    @Mock
    private com.atlassian.jira.config.properties.ApplicationProperties jiraApplicationProperties;
    @Mock
    private I18nResolver i18nResolver;
    @Mock
    private AvatarService avatarService;
    @Mock
    private I18nHelper i18nHelper;
    @Mock
    private JiraAuthenticationContext jiraAuthenticationContext;

    @Mock
    private SlackSettingService slackSettingService;

    @Mock
    private ApplicationUser applicationUser;
    @Mock
    private Issue issue;
    @Mock
    private Status status;
    @Mock
    private Priority priority;
    @Mock
    private Project project;
    @Mock
    private Comment comment;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private DefaultAttachmentHelper target;

    @Before
    public void setUp() {
        when(jiraAuthenticationContext.getI18nHelper()).thenReturn(i18nHelper);
        when(slackSettingService.isInstancePublic()).thenReturn(true);
        target = new DefaultAttachmentHelper(salApplicationProperties, jiraApplicationProperties, i18nResolver,
                avatarService, jiraAuthenticationContext, slackSettingService);
    }

    @Test
    public void buildIssueAttachment() {
        when(issue.getKey()).thenReturn("K");
        when(issue.getSummary()).thenReturn("S");
        when(issue.getAssignee()).thenReturn(applicationUser);
        when(issue.getPriority()).thenReturn(priority);
        when(issue.getProjectObject()).thenReturn(project);
        when(issue.getStatus()).thenReturn(status);
        when(applicationUser.getUsername()).thenReturn("user");
        when(applicationUser.getDisplayName()).thenReturn("UN");
        when(priority.getNameTranslation(i18nHelper)).thenReturn("P");
        when(status.getNameTranslation(i18nHelper)).thenReturn(null);
        when(status.getName()).thenReturn("S");
        when(project.getKey()).thenReturn("PK");
        when(project.getName()).thenReturn("Proj");
        when(i18nResolver.getText("jira.slack.card.notification.issue.title", "K", "S")).thenReturn("title");
        when(i18nResolver.getText("issue.field.status")).thenReturn("Status");
        when(i18nResolver.getText("issue.field.assignee")).thenReturn("Ass");
        when(i18nResolver.getText("issue.field.priority")).thenReturn("Pri");
        when(salApplicationProperties.getBaseUrl(UrlMode.CANONICAL)).thenReturn("baseUrl");
        when(jiraApplicationProperties.getString(APKeys.JIRA_TITLE)).thenReturn("Site");
        when(avatarService.getProjectAvatarAbsoluteURL(project, Avatar.Size.SMALL))
                .thenReturn(URI.create("https://jira.com/context/avatar.png"));

        Field f = Field.builder().title("t").value("v").build();
        Attachment result = target.buildIssueAttachment("pt", issue, Collections.singletonList(f));

        assertThat(result.getPretext(), is("pt"));
        assertThat(result.getTitle(), is("title"));
        assertThat(result.getTitleLink(), startsWith("baseUrl/browse/K"));
        assertThat(result.getText(), is("Status: `S`       Ass: *<baseUrl/secure/ViewProfile.jspa?name=user&atlLinkOrigin=c2xhY2staW50ZWdyYXRpb258dXNlcg%3D%3D|UN>*       Pri: *P*"));
        assertThat(result.getFallback(), is("K pt"));
        assertThat(result.getFooter(), is("<baseUrl/projects/PK?atlLinkOrigin=c2xhY2staW50ZWdyYXRpb258cHJvamVjdA%3D%3D|Proj> | <baseUrl?atlLinkOrigin=c2xhY2staW50ZWdyYXRpb258c2l0ZQ%3D%3D|Site>"));
        assertThat(result.getFooterIcon(), is("https://jira.com/context/avatar.png?format=png"));
        assertThat(result.getColor(), is("#2684FF"));
        assertThat(result.getFields(), contains(f));
    }

    @Test
    public void buildIssueAttachment_withPriorityNullAndAnonymousUserNoPretextNoSiteTitle() {
        when(issue.getKey()).thenReturn("K");
        when(issue.getSummary()).thenReturn("S");
        when(issue.getPriority()).thenReturn(null);
        when(issue.getProjectObject()).thenReturn(project);
        when(issue.getStatus()).thenReturn(status);
        when(status.getNameTranslation(i18nHelper)).thenReturn(null);
        when(status.getName()).thenReturn("S");
        when(project.getKey()).thenReturn("PK");
        when(project.getName()).thenReturn("Proj");
        when(i18nResolver.getText("jira.slack.card.notification.issue.title", "K", "S")).thenReturn("title");
        when(i18nResolver.getText("issue.field.status")).thenReturn("Status");
        when(i18nResolver.getText("issue.field.assignee")).thenReturn("Ass");
        when(i18nResolver.getText("common.concepts.unassigned")).thenReturn("Unas");
        when(salApplicationProperties.getBaseUrl(UrlMode.CANONICAL)).thenReturn("http://baseUrl");
        when(jiraApplicationProperties.getString(APKeys.JIRA_TITLE)).thenReturn("J");
        when(avatarService.getProjectAvatarAbsoluteURL(project, Avatar.Size.SMALL))
                .thenReturn(URI.create("https://jira.com/context/avatar.png"));

        Field f = Field.builder().title("t").value("v").build();
        Attachment result = target.buildIssueAttachment(null, issue, Collections.singletonList(f));

        assertThat(result.getPretext(), nullValue());
        assertThat(result.getTitle(), is("title"));
        assertThat(result.getTitleLink(), startsWith("http://baseUrl/browse/K"));
        assertThat(result.getText(), is("Status: `S`       Ass: *Unas*       "));
        assertThat(result.getFallback(), is("K S"));
        assertThat(result.getFooter(), is("<http://baseUrl/projects/PK?atlLinkOrigin=c2xhY2staW50ZWdyYXRpb258cHJvamVjdA%3D%3D|Proj> | <http://baseUrl/?atlLinkOrigin=c2xhY2staW50ZWdyYXRpb258c2l0ZQ%3D%3D|J>"));
        assertThat(result.getFooterIcon(), is("https://jira.com/context/avatar.png?format=png"));
        assertThat(result.getColor(), is("#2684FF"));
        assertThat(result.getFields(), contains(f));
    }

    @Test
    public void buildCommentAttachment() {
        when(issue.getKey()).thenReturn("K");
        when(issue.getSummary()).thenReturn("S");
        when(issue.getProjectObject()).thenReturn(project);
        when(comment.getId()).thenReturn(9L);
        when(comment.getBody()).thenReturn("comm");
        when(project.getKey()).thenReturn("PK");
        when(project.getName()).thenReturn("Proj");
        when(i18nResolver.getText("jira.slack.card.notification.issue.title", "K", "S")).thenReturn("title");
        when(salApplicationProperties.getBaseUrl(UrlMode.CANONICAL)).thenReturn("baseUrl");
        when(jiraApplicationProperties.getString(APKeys.JIRA_TITLE)).thenReturn("Site");
        when(avatarService.getProjectAvatarAbsoluteURL(project, Avatar.Size.SMALL))
                .thenReturn(URI.create("https://jira.com/context/avatar.png"));

        Attachment result = target.buildCommentAttachment("pt", issue, comment);

        assertThat(result.getPretext(), is("pt"));
        assertThat(result.getTitle(), is("title"));
        assertThat(result.getTitleLink(), is("baseUrl/browse/K?focusedCommentId=9&atlLinkOrigin=c2xhY2staW50ZWdyYXRpb258Y29tbWVudA%3D%3D#comment-9"));
        assertThat(result.getText(), is("comm"));
        assertThat(result.getFallback(), is("K: pt"));
        assertThat(result.getFooter(), is("<baseUrl/projects/PK?atlLinkOrigin=c2xhY2staW50ZWdyYXRpb258cHJvamVjdA%3D%3D|Proj> | <baseUrl?atlLinkOrigin=c2xhY2staW50ZWdyYXRpb258c2l0ZQ%3D%3D|Site>"));
        assertThat(result.getFooterIcon(), is("https://jira.com/context/avatar.png?format=png"));
        assertThat(result.getColor(), is("#2684FF"));
    }

    @Test
    public void jiraUrl() {
        when(salApplicationProperties.getBaseUrl(UrlMode.CANONICAL)).thenReturn("baseUrl");

        assertThat(target.jiraUrl(), is("baseUrl?atlLinkOrigin=c2xhY2staW50ZWdyYXRpb258c2l0ZQ%3D%3D"));
    }

    @Test
    public void userLink_isAnonymousWhenUserIsNull() {
        when(i18nResolver.getText("plugins.slack.common.anonymous")).thenReturn("anon");

        assertThat(target.userLink(null), is("anon"));
    }

    @Test
    public void getAccountMessage() {
        when(applicationUser.getUsername()).thenReturn("user");
        when(applicationUser.getDisplayName()).thenReturn("UN");
        when(salApplicationProperties.getBaseUrl(UrlMode.CANONICAL)).thenReturn("baseUrl");
        when(i18nResolver.getText(
                "jira.plugins.slack.commands.user.link.account.details.message",
                "baseUrl/secure/ViewProfile.jspa?name=user&atlLinkOrigin=c2xhY2staW50ZWdyYXRpb258dXNlcg%3D%3D|UN",
                "baseUrl/plugins/servlet/slack/view-oauth-sessions"
        )).thenReturn("msg");

        assertThat(target.getAccountMessage(applicationUser), is("msg"));
    }

    @Test
    public void getAccountMessage_noAccountMessage() {
        when(salApplicationProperties.getBaseUrl(UrlMode.CANONICAL)).thenReturn("baseUrl");
        when(i18nResolver.getText("jira.plugins.slack.commands.user.link.no.account.message",
                "baseUrl/plugins/servlet/slack/view-oauth-sessions")).thenReturn("msg");

        assertThat(target.getAccountMessage(null), is("msg"));
    }

    @Test
    public void issueLink() {
        when(salApplicationProperties.getBaseUrl(UrlMode.CANONICAL)).thenReturn("baseUrl");
        when(issue.getKey()).thenReturn("issue-key");
        when(issue.getSummary()).thenReturn("issue-summary");
        when(i18nResolver.getText("jira.slack.card.notification.issue.title", "issue-key", "issue-summary"))
                .thenReturn("msg");

        String link = target.issueLink(issue);

        assertThat(link, is(" <baseUrl/browse/issue-key?atlLinkOrigin=c2xhY2staW50ZWdyYXRpb258aXNzdWU%3D|msg>"));
    }
}
