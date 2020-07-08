package com.atlassian.plugin.slack.jira.compat;

import com.atlassian.jira.event.issue.IssueEvent;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Jira8IssueEventWrapper {
    public static boolean isSpanningOperation(final IssueEvent issueEvent) {
        return issueEvent.getSpanningOperation().isPresent();
    }
}
