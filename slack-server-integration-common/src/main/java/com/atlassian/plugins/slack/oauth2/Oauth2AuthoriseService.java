package com.atlassian.plugins.slack.oauth2;

import com.atlassian.sal.api.user.UserKey;
import io.atlassian.fugue.Either;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;

/**
 * Encapsulate the oauth2 process. This component is expected to be called in a request context, as it requires the
 * user's interaction to complete.
 */
public interface Oauth2AuthoriseService {
    /**
     * Start the Oauth2 request, with the given data
     *
     * @param oauth2BeginData oauth magic
     * @return a url to redirect the user to continue the oauth2 process in the browser
     */
    Either<Throwable, URI> beginOauth2(final Oauth2BeginData oauth2BeginData);

    /**
     * Completes the oauth2 process, and inserts the appropriate data to link a confluence user to Slack.
     *
     * @param data oauth magic
     * @return a uri to redirect the user's browser back to. This is typically the screen that initiated the oauth2
     * process.
     */

    Either<Throwable, URI> completeOauth2(final Oauth2CompleteData data);

    URI rejectedOAuth2(String teamId, String state, String error, HttpServletRequest request);

    /**
     * Removes the oauth link for the specified user.
     *
     * @param userKey the application user's key
     * @return true if their configuration existed
     */
    boolean removeOauth2Configuration(UserKey userKey, String teamId);
}
