package com.atlassian.confluence.plugins.slack.util.compat;

import com.atlassian.confluence.search.v2.ISearch;
import com.atlassian.confluence.user.ConfluenceUser;

import javax.annotation.Nullable;

public interface ConfluenceCompatibilityHandler {
    ISearch buildSearch(String query, @Nullable ConfluenceUser confluenceUser, int offset, int limit);
}
