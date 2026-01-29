package com.atlassian.jira.plugins.slack.service.issuefilter.impl;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.index.IndexException;
import com.atlassian.jira.issue.index.IssueIndexingParams;
import com.atlassian.jira.issue.index.IssueIndexingService;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.plugins.slack.bridge.jql.JqlSearcher;
import com.atlassian.jira.plugins.slack.model.EventFilterType;
import com.atlassian.jira.plugins.slack.model.event.JiraIssueEvent;
import com.atlassian.jira.plugins.slack.service.issuefilter.IssueFilter;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.ImportUtils;
import com.atlassian.query.Query;
import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.validation.constraints.NotNull;
import java.util.Optional;

/**
 * We validate if a query applies to the following filter
 */
@Service
public class JqlIssueFilter implements IssueFilter {
    public static final int ISSUE_SEARCH_RETRY_DELAY_SECONDS = 2;

    private static final Logger log = LoggerFactory.getLogger(JqlIssueFilter.class);

    private final SearchService searchService;
    private final JqlSearcher searcher;
    private final IssueIndexingService indexingService;
    private final Sleeper sleeper;

    // Constructor for normal usage (Spring will use this)
    @Autowired
    public JqlIssueFilter(SearchService searchService, JqlSearcher searcher, IssueIndexingService indexingService) {
        this(searchService, searcher, indexingService, new ThreadSleeper());
    }

    // Constructor for testing
    public JqlIssueFilter(SearchService searchService, JqlSearcher searcher, IssueIndexingService indexingService, Sleeper sleeper) {
        this.searchService = searchService;
        this.searcher = searcher;
        this.indexingService = indexingService;
        this.sleeper = sleeper;
    }

    public interface Sleeper {
        void sleep(long milliseconds) throws InterruptedException;
    }

    private static class ThreadSleeper implements Sleeper {
        @Override
        public void sleep(long milliseconds) throws InterruptedException {
            Thread.sleep(milliseconds);
        }
    }

    @Override
    public boolean apply(final JiraIssueEvent event, final @NotNull String value) {
        Issue issue = event.getIssue();
        try {
            boolean isIssueMatched = matchesJql(value, issue, event.getEventAuthor());
            log.debug("JQL matching result for issue key={}, id={}, source={}: {}", issue.getKey(), issue.getId(),
                    event.getSource(), isIssueMatched);

            return isIssueMatched;
        } catch (SearchException e) {
            log.warn(" Search exception trying to match jql [{}] over issue key={}, id={}, source={}", value, issue.getKey(),
                    issue.getId(), event.getSource());
        }
        return false;
    }

    /**
     * We match a jql to a particular issue, we check the version of Jira to improve performance and guarantee that the
     * issue is indexed
     *
     * @param jql    the jql
     * @param issue  the issue
     * @param caller the caller
     * @return true if the query matches, false if not
     * @throws SearchException when things go wrong
     */
    public boolean matchesJql(final String jql,
                              final Issue issue,
                              final Optional<ApplicationUser> caller) throws SearchException {
        if (Strings.isNullOrEmpty(jql)) {
            return true;
        }

        final SearchService.ParseResult parseResult = searchService.parseQuery(caller.orElse(null), jql);
        Boolean result = false;
        if (parseResult.isValid()) {
            // reindex the issue just once regardless of the number of queries to cache to improve performance
            // each reindex fires an event, that may be handled Jira or in other apps
            // too many reindex queries make Jira slower
            reIndexIssue(issue);

            // check if the issue is indexed and wait otherwise
            int i = 0;
            int maxAttempts = 3;
            while (i++ < maxAttempts) {
                final Query query = JqlQueryBuilder.newBuilder()
                        .where()
                        .and()
                        .issue()
                        .eq(issue.getKey())
                        .buildQuery();

                if (searcher.doesIssueMatchQuery(issue, null, query)) {
                    log.debug("Issue key={} is found in index", issue.getKey());
                    break;
                }

                try {
                    log.debug("Issue key={} is not found in index. Attempt #{}.{}", issue.getKey(), i,
                            i < maxAttempts ? " Retrying in " + ISSUE_SEARCH_RETRY_DELAY_SECONDS + " seconds" : "");
                    sleeper.sleep(ISSUE_SEARCH_RETRY_DELAY_SECONDS * 1000);
                } catch (InterruptedException e) {
                    // no-op
                }
            }

            final Query query = JqlQueryBuilder.newBuilder(parseResult.getQuery())
                    .where()
                    .and()
                    .issue()
                    .eq(issue.getKey())
                    .buildQuery();

            // Passing no user, so that we search with no security constraints.
            // The creator of the issue does not necessarily have access to the ticket, failing the search.
            // See https://jira.atlassian.com/browse/HC-26371 for more details.
            result = searcher.doesIssueMatchQuery(issue, null, query);
            log.debug("Running JQL [{}] for issue [{}] : {}", query.toString(), issue, result);
        }

        return result;
    }

    private void reIndexIssue(final Issue issue) {
        final boolean wasIndexingIssues = ImportUtils.isIndexIssues();
        if (!wasIndexingIssues) {
            ImportUtils.setIndexIssues(true);
        }

        try {
            indexingService.reIndex(issue, IssueIndexingParams.INDEX_ISSUE_ONLY);
        } catch (IndexException e) {
            log.error("An error occurred during the issue key={} reindex", issue.getKey(), e);
        } finally {
            if (!wasIndexingIssues) {
                ImportUtils.setIndexIssues(false);
            }
        }
    }

    @Override
    public EventFilterType getEventFilterType() {
        return EventFilterType.JQL_QUERY;
    }
}
