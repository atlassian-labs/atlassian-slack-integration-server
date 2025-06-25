package com.atlassian.plugins.slack.rest;

import com.atlassian.plugins.rest.api.security.annotation.AnonymousSiteAccess;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.UrlMode;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import static jakarta.ws.rs.core.Response.Status.MOVED_PERMANENTLY;

@Path("/settings")
@AnonymousSiteAccess
public class SlackSettingsRedirectResource {
    private final ApplicationProperties applicationProperties;

    @Inject
    public SlackSettingsRedirectResource(@Named("salApplicationProperties") final ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    /**
     * This method redirects users coming from Slack's custom-install edit page - which links to
     * {@code https://<domain>/slack/settings} - to the global configuration page.
     */
    @GET
    public Response redirectToGlobalConfig() {
        return Response.status(MOVED_PERMANENTLY)
                .location(UriBuilder
                        .fromPath(applicationProperties.getBaseUrl(UrlMode.ABSOLUTE))
                        .path("/plugins/servlet/slack/configure")
                        .build())
                .build();
    }
}
