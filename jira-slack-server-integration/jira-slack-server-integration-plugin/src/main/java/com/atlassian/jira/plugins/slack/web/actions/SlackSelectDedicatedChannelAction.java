package com.atlassian.jira.plugins.slack.web.actions;

import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.security.request.RequestMethod;
import com.atlassian.jira.security.request.SupportedMethods;
import com.atlassian.jira.web.action.ActionViewDataMappings;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.plugins.slack.api.client.SlackClient;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.google.common.collect.ImmutableMap;
import io.atlassian.fugue.Either;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Provides context for dedicated channel selection dialog.
 */
@SupportedMethods(RequestMethod.GET)
public class SlackSelectDedicatedChannelAction extends JiraWebActionSupport {
    private final IssueManager issueManager;
    private final SlackLinkManager slackLinkManager;
    private final SlackClientProvider slackClientProvider;

    private String issueKey;

    public SlackSelectDedicatedChannelAction(final IssueManager issueManager,
                                             final SlackLinkManager slackLinkManager,
                                             final SlackClientProvider slackClientProvider) {
        this.issueManager = issueManager;
        this.slackLinkManager = slackLinkManager;
        this.slackClientProvider = slackClientProvider;
    }

    @Override
    protected String doExecute() {
        final MutableIssue issue = issueManager.getIssueByKeyIgnoreCase(issueKey);
        if (issue == null) {
            return ERROR;
        }

        return SUCCESS;
    }

    @ActionViewDataMappings({SUCCESS})
    public Map<String, Object> getData() {
        return ImmutableMap.<String, Object>builder()
                .put("confirmedLinks", slackLinkManager.getLinks().stream()
                        .filter(link -> slackClientProvider
                                .withLink(link)
                                .withRemoteUserTokenIfAvailable()
                                .map(SlackClient::testToken)
                                .filter(Either::isRight)
                                .isPresent())
                        .collect(Collectors.toList()))
                .build();
    }

    public String getIssueKey() {
        return issueKey;
    }

    public void setIssueKey(String issueKey) {
        this.issueKey = issueKey;
    }
}
