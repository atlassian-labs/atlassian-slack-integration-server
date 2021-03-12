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
    public boolean matches(final Issue issue, final ProjectConfiguration config) {
        final String issueStatusId = issue.getStatus().getId();
        final String selectedStatusesIds = config.getValue();
        final boolean isIssueMatched = new MultipleValue(selectedStatusesIds).apply(issueStatusId);

        if (!isIssueMatched) {
            log.debug("Issue key={}, id={} with status id={} isn't matched because status is absent in selected statuses list: [{}]",
                    issue.getKey(), issue.getId(), issueStatusId, selectedStatusesIds);
        }

        return isIssueMatched;
    }
}
