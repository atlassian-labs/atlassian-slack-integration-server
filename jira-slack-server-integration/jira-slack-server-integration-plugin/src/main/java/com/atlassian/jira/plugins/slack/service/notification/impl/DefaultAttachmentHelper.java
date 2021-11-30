package com.atlassian.jira.plugins.slack.service.notification.impl;

import com.atlassian.jira.avatar.Avatar;
import com.atlassian.jira.avatar.AvatarService;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueConstant;
import com.atlassian.jira.issue.comments.Comment;
import com.atlassian.jira.plugins.slack.service.notification.AttachmentHelper;
import com.atlassian.jira.plugins.slack.util.CommentUtil;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.UriBuilder;
import java.util.Arrays;
import java.util.List;

import static com.atlassian.plugins.slack.util.LinkHelper.decorateWithOrigin;
import static com.atlassian.plugins.slack.util.SlackHelper.escapeSignsForSlackLink;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.defaultString;

@Component
public class DefaultAttachmentHelper implements AttachmentHelper {
    private final ApplicationProperties applicationProperties;
    private final com.atlassian.jira.config.properties.ApplicationProperties jiraApplicationProperties;
    private final I18nResolver i18nResolver;
    private final AvatarService avatarService;
    private final I18nHelper i18nHelper;

    private final SlackSettingService slackSettingService;

    private final boolean includeImages;

    @Autowired
    public DefaultAttachmentHelper(
            @Qualifier("salApplicationProperties") final ApplicationProperties applicationProperties,
            @Qualifier("jiraApplicationProperties") final com.atlassian.jira.config.properties.ApplicationProperties jiraApplicationProperties,
            final I18nResolver i18nResolver,
            final AvatarService avatarService,
            final JiraAuthenticationContext jiraAuthenticationContext,
            final SlackSettingService slackSettingService) {
        this.applicationProperties = applicationProperties;
        this.jiraApplicationProperties = jiraApplicationProperties;
        this.i18nResolver = i18nResolver;
        this.avatarService = avatarService;
        this.i18nHelper = jiraAuthenticationContext.getI18nHelper();
        this.slackSettingService = slackSettingService;

        includeImages = Boolean.parseBoolean(System.getProperty("slack.notification.include.images", "true"));
    }

    @Override
    public Attachment buildIssueAttachment(final String pretext, final Issue issue, List<Field> fields) {
        final String status = getIssueConstantText(issue.getStatus());
        final String priority = getIssueConstantText(issue.getPriority());
        final String assignee = issue.getAssignee() == null
                ? i18nResolver.getText("common.concepts.unassigned")
                : "<" + userLink(issue.getAssignee()) + ">";

        Project project = issue.getProjectObject();
        Attachment attachment = Attachment.builder()
                .pretext(pretext)
                .title(i18nResolver.getText("jira.slack.card.notification.issue.title", issue.getKey(), issue.getSummary()))
                .titleLink(issueUrl(issue.getKey()))
                .fallback(issue.getKey() + " " + defaultIfBlank(pretext, issue.getSummary()))
                .text(i18nResolver.getText("issue.field.status") + ": `" + status + "`       " +
                        i18nResolver.getText("issue.field.assignee") + ": *" + assignee + "*       " +
                        (priority != null ? i18nResolver.getText("issue.field.priority") + ": *" + priority + "*" : ""))
                .footer(footerText(project))
                .color("#2684FF")
                .fields(fields)
                .mrkdwnIn(Arrays.asList("text", "pretext"))
                .build();

        // https://pi-dev-sandbox.atlassian.net/browse/JSS-8
        // Slack tries to load the image for 10 seconds before posing a message;
        // for BTF instances image loading always fails, but user still have to wait for 10 seconds for every message
        // before a message appears in a Slack channel; so just remove image from the message for private instances
        if (includeImages && slackSettingService.isInstancePublic()) {
            attachment.setFooterIcon(projectIcon(project));
        }

        return attachment;
    }

    private String getIssueConstantText(final IssueConstant issueConstant) {
        if (issueConstant == null) {
            return null;
        }
        return defaultIfBlank(issueConstant.getNameTranslation(i18nHelper), issueConstant.getName());
    }

    @Override
    public Attachment buildCommentAttachment(final String pretext, final Issue issue, final Comment comment) {
        String cleanCommentBody = CommentUtil.removeJiraTags(comment.getBody());
        return Attachment.builder()
                .pretext(pretext)
                .title(i18nResolver.getText("jira.slack.card.notification.issue.title", issue.getKey(), issue.getSummary()))
                .titleLink(decorateWithOrigin(UriBuilder.fromUri(issueUrl(issue.getKey()))
                        .queryParam("focusedCommentId", comment.getId())
                        .fragment("comment-" + comment.getId())
                        .build()
                        .toString(), "comment"))
                .fallback(issue.getKey() + ": " + defaultString(pretext))
                .text(cleanCommentBody)
                .footer(footerText(issue.getProjectObject()))
                .footerIcon(projectIcon(issue.getProjectObject()))
                .color("#2684FF")
                .mrkdwnIn(Arrays.asList("text", "pretext"))
                .build();
    }

    private String footerText(final Project project) {
        return "<" + projectUrl(project.getKey()) + "|" + escapeSignsForSlackLink(project.getName()) + "> | <"
                + jiraUrl() + "|" + escapeSignsForSlackLink(jiraTitle()) + ">";
    }

    private String projectIcon(final Project project) {
        return UriBuilder.fromUri(avatarService.getProjectAvatarAbsoluteURL(project, Avatar.Size.SMALL))
                .queryParam("format", "png")
                .build()
                .toString();
    }

    @Override
    public String jiraTitle() {
        return defaultIfBlank(jiraApplicationProperties.getString(APKeys.JIRA_TITLE), applicationProperties.getDisplayName());
    }

    @Override
    public String jiraUrl() {
        return decorateWithOrigin(baseUrl(), "site");
    }

    @Override
    public String issueUrl(final String issueKey) {
        return decorateWithOrigin(baseUrl() + "/browse/" + issueKey, "issue");
    }

    @Override
    public String issueLink(final Issue issue) {
        // leading space is left here intentionally in order to make this link optional in notifications
        // without breaking spacing
        return String.format(" <%s|%s>", issueUrl(issue.getKey()),
                i18nResolver.getText("jira.slack.card.notification.issue.title", issue.getKey(), issue.getSummary()));
    }

    @Override
    public String userLink(final ApplicationUser user) {
        if (user != null) {
            return decorateWithOrigin(baseUrl() + "/secure/ViewProfile.jspa?name=" + user.getUsername(), "user")
                    + "|" + escapeSignsForSlackLink(user.getDisplayName());
        } else {
            return i18nResolver.getText("plugins.slack.common.anonymous");
        }
    }

    @Override
    public String projectUrl(final String projectKey) {
        return decorateWithOrigin(baseUrl() + "/projects/" + projectKey, "project");
    }

    @Override
    public String getAccountMessage(final ApplicationUser user) {
        if (user != null) {
            return i18nResolver.getText(
                    "jira.plugins.slack.commands.user.link.account.details.message",
                    userLink(user),
                    oAuthSessionsPageUrl());
        }
        return i18nResolver.getText("jira.plugins.slack.commands.user.link.no.account.message", oAuthSessionsPageUrl());
    }

    private String oAuthSessionsPageUrl() {
        return baseUrl() + "/plugins/servlet/slack/view-oauth-sessions";
    }

    private String baseUrl() {
        return applicationProperties.getBaseUrl(UrlMode.CANONICAL);
    }
}
