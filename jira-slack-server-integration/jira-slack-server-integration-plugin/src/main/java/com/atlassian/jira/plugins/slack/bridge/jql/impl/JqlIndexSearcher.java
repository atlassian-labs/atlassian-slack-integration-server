package com.atlassian.jira.plugins.slack.bridge.jql.impl;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.index.ThreadLocalSearcherCache;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchProvider;
import com.atlassian.jira.issue.search.SearchQuery;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.plugins.slack.bridge.jql.JqlSearcher;
import com.atlassian.jira.plugins.slack.compat.FixRequestTypeClauseVisitor;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.query.Query;
import com.atlassian.query.clause.Clause;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;

/**
 * This jql index searcher commits the issue to the FS and runs the query.
 */
@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class JqlIndexSearcher implements JqlSearcher {
    private static final Logger log = LoggerFactory.getLogger(JqlIndexSearcher.class);

    private final SearchProvider searchProvider;

    @Override
    public Boolean doesIssueMatchQuery(final Issue issue,
                                       @Nullable final ApplicationUser caller,
                                       final Query query) throws SearchException {
        try {
            ThreadLocalSearcherCache.startSearcherContext();
            long searchCount;

            Query fixedQuery = fixRequestTypeInQuery(query, issue.getProjectObject());
            SearchQuery searchQuery = SearchQuery.create(fixedQuery, caller);
            if (caller == null) {
                searchQuery.overrideSecurity(true);
            }
            searchCount = searchProvider.getHitCount(searchQuery);

            return searchCount > 0;
        } catch (SearchException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            ThreadLocalSearcherCache.stopAndCloseSearcherContext();
        }
    }

    private Query fixRequestTypeInQuery(final Query query, final Project project) {
        Clause oldWhereClause = query.getWhereClause();
        if (oldWhereClause == null) {
            return query;
        }

        FixRequestTypeClauseVisitor visitor = new FixRequestTypeClauseVisitor(project);
        Clause fixedWhereClause = oldWhereClause.accept(visitor);

        Query fixedQuery = query;
        if (visitor.isClauseChanged()) {
            log.debug("JQL query before fixing a request type: [{}]", query.toString());
            fixedQuery = JqlQueryBuilder.newBuilder()
                    .where()
                    .addClause(fixedWhereClause)
                    .endWhere()
                    .orderBy()
                    .setSorts(query.getOrderByClause())
                    .buildQuery();
            log.debug("JQL query after fixing a request type: [{}]", fixedQuery.toString());
        }

        return fixedQuery;
    }
}
