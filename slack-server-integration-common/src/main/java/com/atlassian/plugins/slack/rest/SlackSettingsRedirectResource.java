package com.atlassian.plugins.slack.rest;

import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.UrlMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import static javax.ws.rs.core.Response.Status.MOVED_PERMANENTLY;

@Path("/settings")
@AnonymousAllowed
public class SlackSettingsRedirectResource {
    private final ApplicationProperties applicationProperties;

    @Autowired
    public SlackSettingsRedirectResource(@Qualifier("salApplicationProperties") final ApplicationProperties applicationProperties) {
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
