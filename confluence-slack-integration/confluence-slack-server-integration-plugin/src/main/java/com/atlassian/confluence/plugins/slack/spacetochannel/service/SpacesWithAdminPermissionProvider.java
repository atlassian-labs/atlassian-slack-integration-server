package com.atlassian.confluence.plugins.slack.spacetochannel.service;

import com.atlassian.confluence.plugins.slack.spacetochannel.model.SpaceResult;
import com.atlassian.confluence.user.ConfluenceUser;
import io.atlassian.fugue.Either;

import java.util.List;

public interface SpacesWithAdminPermissionProvider {
    /**
     * Find a list of spaces with names matching the input query for which the specified user is a space admin.
     *
     * @param name       The name of the space to query for. (Must not be null).
     * @param user       The user to query as. Spaces will only be returned if the user is a space admin. (Must not be null).
     * @param maxResults The maximum number of results we want back from this query. (Must be &gt; 0).
     * @return success() containing an iterable of space results if we were able to successfully query the space
     * list, otherwise error().
     */
    Either<Exception, List<SpaceResult>> findSpacesMatchingName(String name, ConfluenceUser user, int maxResults);

}
