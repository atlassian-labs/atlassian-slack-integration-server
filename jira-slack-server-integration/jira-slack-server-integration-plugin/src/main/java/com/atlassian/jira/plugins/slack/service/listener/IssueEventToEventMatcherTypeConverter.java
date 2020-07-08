package com.atlassian.jira.plugins.slack.service.listener;

import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.plugins.slack.model.EventMatcherType;

import java.util.Collection;


public interface IssueEventToEventMatcherTypeConverter {

    /**
     * Find the corresponding EventMatcherType for a given Jira issue event.
     * Some events like ISSUE_UPDATED can results in multiple EventMatcherType depending
     * on the updated fields
     */
    Collection<EventMatcherType> match(IssueEvent issueEvent);
}
