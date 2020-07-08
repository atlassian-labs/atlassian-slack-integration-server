package com.atlassian.plugins.slack.spi;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Optional;

/**
 * Manager that determines the URI to redirect the ConfigureServlet to
 * if any.
 */
public interface ConfigurationRedirectionManager {

    /**
     * Returns a defined option with the URI to redirect to
     * based on the request.
     *
     * @param request the given request
     * @return a defined option with the URI to redirect to. If
     * no redirect is needed an empty option is returned.
     */
    Optional<URI> getRedirectUri(HttpServletRequest request);
}
