package com.atlassian.confluence.plugins.slack.util;

import com.atlassian.confluence.search.v2.ContentSearch;
import com.atlassian.confluence.search.v2.ISearch;
import com.atlassian.confluence.search.v2.SearchQuery;
import com.atlassian.confluence.search.v2.SpacePermissionQueryFactory;
import com.atlassian.confluence.search.v2.query.ArchivedSpacesQuery;
import com.atlassian.confluence.search.v2.query.BooleanQuery;
import com.atlassian.confluence.search.v2.query.ContentPermissionsQuery;
import com.atlassian.confluence.search.v2.query.TextQuery;
import com.atlassian.confluence.search.v2.sort.RelevanceSort;
import com.atlassian.confluence.spaces.SpaceManager;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.component.ComponentLocator;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class SearchBuilder {
    @ComponentImport
    private SpacePermissionQueryFactory spacePermissionQueryFactory;
    private SpaceManager spaceManager;

    private SearchBuilder() {
    }

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

        return ContentSearch.builder()
                .query(BooleanQuery.composeAndQuery(searchQueries))
                .sort(new RelevanceSort())
                .startOffset(offset)
                .limit(limit)
                .build();
    }

    protected SpaceManager getSpaceManager() {
        if (spaceManager == null) {
            spaceManager = ComponentLocator.getComponent(SpaceManager.class);
        }
        return spaceManager;
    }

    private SpacePermissionQueryFactory getSpacePermissionQueryFactory() {
        if (spacePermissionQueryFactory == null) {
            spacePermissionQueryFactory = ComponentLocator.getComponent(SpacePermissionQueryFactory.class);
        }

        return spacePermissionQueryFactory;
    }
}
