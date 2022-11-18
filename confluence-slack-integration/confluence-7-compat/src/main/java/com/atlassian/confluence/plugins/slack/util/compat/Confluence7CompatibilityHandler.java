package com.atlassian.confluence.plugins.slack.util.compat;

import com.atlassian.confluence.search.v2.ContentSearch;
import com.atlassian.confluence.search.v2.ISearch;
import com.atlassian.confluence.search.v2.SearchFilter;
import com.atlassian.confluence.search.v2.query.TextQuery;
import com.atlassian.confluence.search.v2.searchfilter.ArchivedSpacesSearchFilter;
import com.atlassian.confluence.search.v2.searchfilter.ContentPermissionsSearchFilter;
import com.atlassian.confluence.search.v2.searchfilter.SpacePermissionsSearchFilter;
import com.atlassian.confluence.search.v2.sort.RelevanceSort;
import com.atlassian.confluence.user.ConfluenceUser;

import javax.annotation.Nullable;

public class Confluence7CompatibilityHandler extends BaseConfluenceCompatibilityHandler
        implements ConfluenceCompatibilityHandler {
    public static final Confluence7CompatibilityHandler INSTANCE = new Confluence7CompatibilityHandler();

    private Confluence7CompatibilityHandler() {}

    @Override
    public ISearch buildSearch(final String query, @Nullable final ConfluenceUser confluenceUser, final int offset,
                               final int limit) {
        TextQuery textQuery = new TextQuery(query);
        SearchFilter filter = ContentPermissionsSearchFilter.getInstance()
                .and(new ArchivedSpacesSearchFilter(false, getSpaceManager()))
                .and(new SpacePermissionsSearchFilter(null, null));
        RelevanceSort sort = new RelevanceSort();
        ISearch searchConfig = new ContentSearch(textQuery, sort, filter, offset, limit);

        return searchConfig;
    }
}
