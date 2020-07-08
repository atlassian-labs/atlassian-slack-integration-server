package com.atlassian.plugin.slack.jira.compat;

import com.atlassian.jira.issue.index.ThreadLocalSearcherCache;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchProvider;
import com.atlassian.jira.issue.search.SearchQuery;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.query.Query;
import lombok.experimental.UtilityClass;

import javax.annotation.Nullable;

@UtilityClass
public class Jira8JqlIndexSearcher {
    public static long searchCount(final SearchProvider searchProvider,
                                   @Nullable final ApplicationUser caller,
                                   final Query query) throws SearchException {
        try {
            ThreadLocalSearcherCache.startSearcherContext();
            if (caller != null) {
                return searchProvider.getHitCount(SearchQuery.create(query, caller));
            }
            return searchProvider.getHitCount(SearchQuery.create(query, null).overrideSecurity(true));
        } finally {
            ThreadLocalSearcherCache.stopAndCloseSearcherContext();
        }
    }
}
