package com.atlassian.jira.plugins.slack.web.rest;

import com.atlassian.jira.plugins.slack.system.PluginInfoSource;
import com.atlassian.plugins.rest.api.security.annotation.UnrestrictedAccess;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
