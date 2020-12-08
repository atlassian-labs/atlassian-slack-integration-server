package com.atlassian.jira.plugins.slack.service.issuefilter.impl;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.plugins.slack.bridge.jql.JqlSearcher;
import com.atlassian.jira.plugins.slack.model.EventFilterType;
import com.atlassian.jira.plugins.slack.service.issuefilter.IssueFilter;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.query.Query;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
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

    @Autowired
    public JqlIssueFilter(final SearchService searchService,
                          final JqlSearcher searcher) {
        this.searcher = searcher;
        this.searchService = searchService;
    }

    @Override
    public boolean apply(final IssueEvent event, final @NotNull String value) {
        Issue issue = event.getIssue();
        try {
            boolean isIssueMatched = matchesJql(value, issue, Optional.ofNullable(event.getUser()));

            log.debug("Issue key={} eventTypeId={} JQL matching result: {}", issue.getKey(), event.getEventTypeId(), isIssueMatched);

            return isIssueMatched;
        } catch (SearchException e) {
            log.warn(" Search exception trying to match jql [{}] over issue key={}", value, issue.getKey());
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

            // check if the issue is indexed and wait otherwise
            int i = 0;
            while (i++ < 3) {
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
                    log.debug("Issue key={} is not found in index. Attempt #{}. Retrying in {} seconds", issue.getKey(), i,
                            ISSUE_SEARCH_RETRY_DELAY_SECONDS);
                    Thread.sleep(ISSUE_SEARCH_RETRY_DELAY_SECONDS * 1000);
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

    @Override
    public EventFilterType getEventFilterType() {
        return EventFilterType.JQL_QUERY;
    }
}
