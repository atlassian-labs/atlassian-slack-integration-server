package com.atlassian.jira.plugins.slack.web.actions;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.plugins.slack.model.analytics.IssuePanelPopupOpenedEvent;
import com.atlassian.jira.plugins.slack.model.mentions.IssueMentionViewResponseFactory;
import com.atlassian.jira.plugins.slack.model.mentions.MentionChannel;
import com.atlassian.jira.plugins.slack.service.mentions.IssueMentionService;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.request.RequestMethod;
import com.atlassian.jira.security.request.SupportedMethods;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.web.action.ActionViewDataMappings;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.plugins.slack.analytics.AnalyticsContextProvider;
import com.google.common.collect.ImmutableMap;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;

@SupportedMethods(RequestMethod.GET)
@RequiredArgsConstructor
public class SlackIssueMentionsAction extends JiraWebActionSupport {
    private final IssueMentionService issueMentionService;
    private final IssueManager issueManager;
    private final IssueMentionViewResponseFactory responseFactory;
    private final PermissionManager permissionManager;
    private final EventPublisher eventPublisher;
    private final AnalyticsContextProvider analyticsContextProvider;

    private String issueKey;
    private Issue issue;
    private Map<String, Object> data;

    @Override
    protected void doValidation() {
        if (issue == null) {
            addErrorMessage("Issue does not exist.");
        }
    }

    @Override
    protected String doExecute() {
        final ApplicationUser loggedInUser = getLoggedInUser();
        if (loggedInUser == null) {
            return ERROR;
        }

        if (issue == null) {
            return ERROR;
        }
        if (!permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, issue, loggedInUser)) {
            return ERROR;
        }

        return issueMentionService.getIssueMentions(issue.getId()).fold(
                error -> {
                    log.warn("Failed to get issue mention data for issueKey=" + issueKey, error);
                    addErrorMessage(isNullOrEmpty(error.getMessage()) ?
                            "Failed to get issue mention data." : error.getMessage());
                    return ERROR;
                },
                issueMentions -> {
                    final List<IssueMentionViewResponseFactory.IssueMentionViewItem> issueMentionViewItems =
                            responseFactory.createResponse(issueMentions, getLoggedInUser().getKey());
                    final List<MentionChannel> mentionChannels = issueMentionViewItems.stream()
                            .map(IssueMentionViewResponseFactory.IssueMentionViewItem::getChannel)
                            .collect(Collectors.toList());
                    data = ImmutableMap.of("issueMentions", issueMentionViewItems,
                            "mentionChannels", mentionChannels,
                            "totalChannels", mentionChannels.stream().map(c -> c.getKey().getChannelId()).distinct().count());

                    eventPublisher.publish(new IssuePanelPopupOpenedEvent(analyticsContextProvider.current()));

                    return SUCCESS;
                });
    }

    @ActionViewDataMappings({ERROR})
    public Map<String, Object> getErrorDataMap() {
        return ImmutableMap.<String, Object>builder()
                .put("errors", getErrorMessages())
                .build();
    }

    @ActionViewDataMappings({SUCCESS})
    public Map<String, Object> getData() {
        return data == null ? ImmutableMap.of() : data;
    }

    public String getIssueKey() {
        return issueKey;
    }

    public void setIssueKey(String issueKey) {
        this.issueKey = issueKey;
        fetchIssueByKey();
    }

    private void fetchIssueByKey() {
        if (!isNullOrEmpty(issueKey)) {
            issue = issueManager.getIssueByKeyIgnoreCase(issueKey);
        }
    }
}
