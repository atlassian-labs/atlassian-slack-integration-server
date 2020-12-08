package com.atlassian.jira.plugins.slack.util.matcher;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugins.slack.model.ProjectConfiguration;
import com.atlassian.jira.plugins.slack.model.dto.MultipleValue;
import lombok.extern.slf4j.Slf4j;

/**
 * This workflow event matcher will check if the event
 * can be tracked
 */
@Slf4j
public class WorkflowStatusMatcher implements EventMatcher {
    @Override
    public boolean matches(Issue issue, ProjectConfiguration config) {
        final String issueStatusId = issue.getStatus().getId();
        String selectedStatusIds = config.getValue();
        boolean isIssueMatched = new MultipleValue(selectedStatusIds).apply(issueStatusId);
        if (!isIssueMatched) {
            log.debug("Not matching issue key={} with status id={} because it is absent in selected statuses list {}",
                    issue.getKey(), issueStatusId, selectedStatusIds);
        }

        return isIssueMatched;
    }
}
