package com.atlassian.jira.plugins.slack.web.rest;

import com.atlassian.jira.plugins.slack.system.PluginInfoSource;
import com.atlassian.plugins.rest.api.security.annotation.UnrestrictedAccess;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Rest Endpoint that will let us validate the configuration of the plugin
 */
@UnrestrictedAccess
@Path("/info")
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
public class PluginInfoResource {
    @GET
    public Response getPluginInfo() {
        return Response.ok(PluginInfoSource.getPluginInfo()).build();
    }
}
