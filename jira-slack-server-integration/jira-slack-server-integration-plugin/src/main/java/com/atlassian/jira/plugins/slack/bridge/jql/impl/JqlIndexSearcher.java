package com.atlassian.jira.plugins.slack.bridge.jql.impl;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.index.IndexException;
import com.atlassian.jira.issue.index.IssueIndexingService;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchProvider;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.plugins.slack.bridge.jql.JqlSearcher;
import com.atlassian.jira.plugins.slack.compat.FixRequestTypeClauseVisitor;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.ImportUtils;
import com.atlassian.plugin.slack.jira.compat.Jira8JqlIndexSearcher;
import com.atlassian.query.Query;
import com.atlassian.query.clause.Clause;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.Optional;

import static com.atlassian.plugin.slack.jira.compat.WithJira8.withJira8;

/**
 * This jql index searcher commits the issue to the FS and runs the query.
 */
@Service
public class JqlIndexSearcher implements JqlSearcher {
    private static final Logger log = LoggerFactory.getLogger(JqlIndexSearcher.class);

    private final SearchProvider searchProvider;
    private final IssueIndexingService indexingService;

    @Autowired
    public JqlIndexSearcher(final SearchProvider searchProvider,
                            final IssueIndexingService indexingService) {
        this.searchProvider = searchProvider;
        this.indexingService = indexingService;
    }

    @Override
    public Boolean doesIssueMatchQuery(final Issue issue,
                                       @Nullable final ApplicationUser caller,
                                       final Query query) throws SearchException {
        reIndexIssue(issue);

        try {
            Query fixedQuery = fixRequestTypeInQuery(query, issue.getProjectObject());
            final Optional<Long> searchCountOptional = withJira8(() -> Jira8JqlIndexSearcher.searchCount(searchProvider, caller, fixedQuery));

            long searchCount;
            if (searchCountOptional.isPresent()) {
                searchCount = searchCountOptional.get();
            } else if (caller != null) {
                searchCount = searchProvider.searchCount(fixedQuery, caller);
            } else {
                searchCount = searchProvider.searchCountOverrideSecurity(fixedQuery, null);
            }

            return searchCount > 0;
        } catch (SearchException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void reIndexIssue(final Issue issue) {
        final boolean indexIssues = ImportUtils.isIndexIssues();
        ImportUtils.setIndexIssues(true);

        try {
            indexingService.reIndex(issue);
        } catch (IndexException e) {
            log.error("An error occurred during the issue {} reindex", issue.getId(), e);
        } finally {
            ImportUtils.setIndexIssues(indexIssues);
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
