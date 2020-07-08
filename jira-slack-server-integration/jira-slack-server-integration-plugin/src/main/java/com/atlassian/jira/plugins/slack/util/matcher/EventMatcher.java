package com.atlassian.jira.plugins.slack.util.matcher;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugins.slack.model.ProjectConfiguration;

/**
 * Matcher interface for EventMatcherType
 */
public interface EventMatcher {
    /**
     * Checks if the following project configuration applies to
     * the issue
     *
     * @param issue  the issue
     * @param config the project configuration
     * @return if it matches or not
     */
    boolean matches(Issue issue, ProjectConfiguration config);
}
