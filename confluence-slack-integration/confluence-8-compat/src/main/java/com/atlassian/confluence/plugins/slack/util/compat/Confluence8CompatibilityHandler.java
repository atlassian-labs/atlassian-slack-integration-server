package com.atlassian.confluence.plugins.slack.util.compat;

import com.atlassian.confluence.search.v2.ContentSearch;
import com.atlassian.confluence.search.v2.ISearch;
import com.atlassian.confluence.search.v2.SearchQuery;
import com.atlassian.confluence.search.v2.SpacePermissionQueryFactory;
import com.atlassian.confluence.search.v2.query.ArchivedSpacesQuery;
import com.atlassian.confluence.search.v2.query.BooleanQuery;
import com.atlassian.confluence.search.v2.query.ContentPermissionsQuery;
import com.atlassian.confluence.search.v2.query.TextQuery;
import com.atlassian.confluence.search.v2.sort.RelevanceSort;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.component.ComponentLocator;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

public class Confluence8CompatibilityHandler extends BaseConfluenceCompatibilityHandler
        implements ConfluenceCompatibilityHandler {
    public static final Confluence8CompatibilityHandler INSTANCE = new Confluence8CompatibilityHandler();

    // component is imported here instead of ComponentImports class intentionally because SpacePermissionQueryFactory
    // is only available since Confluence 7.17, while ComponentImports are compiled against libraries of older Confluence
    @ComponentImport
    private SpacePermissionQueryFactory spacePermissionQueryFactory;

    private Confluence8CompatibilityHandler() {}

    @Override
    public ISearch buildSearch(final String query, @Nullable final ConfluenceUser confluenceUser, final int offset,
                               final int limit) {
        Set<SearchQuery> searchQueries = new HashSet<>();

        searchQueries.add(new TextQuery(query));
        searchQueries.add(new ArchivedSpacesQuery(false, getSpaceManager()));

        if (confluenceUser != null) {
            searchQueries.add(ContentPermissionsQuery.builder()
                    .user(confluenceUser)
                    .build());
            searchQueries.add(getSpacePermissionQueryFactory().create(confluenceUser));
        }
        SearchQuery searchQuery = BooleanQuery.composeAndQuery(searchQueries);
        RelevanceSort sort = new RelevanceSort();
        ISearch searchConfig = new ContentSearch(searchQuery, sort, offset, limit);

        return searchConfig;
    }

    private SpacePermissionQueryFactory getSpacePermissionQueryFactory() {
        if (spacePermissionQueryFactory == null) {
            spacePermissionQueryFactory = ComponentLocator.getComponent(SpacePermissionQueryFactory.class);
        }

        return spacePermissionQueryFactory;
    }
}
