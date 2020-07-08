package com.atlassian.jira.plugins.slack.service.issuefilter;

import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.plugins.slack.model.ProjectConfiguration;

import java.util.Collection;

/**
 * Service to filter any issue event
 */
public interface IssueFilterService {

    /**
     * We are going to pass an issue event and going to
     * verify if it applies to the filters provided
     * @param event the event
     * @param configurations the filters we are going to run
     * @return true if the issue applies, false if not
     */
    boolean apply(final IssueEvent event, final Collection<ProjectConfiguration> configurations);
}
