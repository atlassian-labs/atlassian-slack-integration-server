package com.atlassian.jira.plugins.slack.bridge.jql;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.query.Query;

import javax.annotation.Nullable;

/**
 * Bridge API for using the TemporaryIndexManager or the normal index provider in Jira
 */
public interface JqlSearcher {
    /**
     * Checks if the issue matches the given query
     *
     * @param issue  the issue
     * @param caller optional caller
     * @param query  the query
     * @return true if the query applies to the issue
     * @throws SearchException an exception, thrown it is
     */
    Boolean doesIssueMatchQuery(Issue issue, @Nullable ApplicationUser caller, Query query) throws SearchException;
}
