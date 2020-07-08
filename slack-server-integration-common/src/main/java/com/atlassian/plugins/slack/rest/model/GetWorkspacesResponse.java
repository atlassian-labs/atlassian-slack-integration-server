package com.atlassian.plugins.slack.rest.model;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

import static org.codehaus.jackson.annotate.JsonAutoDetect.Visibility.ANY;

@SuppressWarnings("unused")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = ANY)
public final class GetWorkspacesResponse {
    private final List<LimitedSlackLinkDto> workspaces;

    public GetWorkspacesResponse(@JsonProperty("workspaces") final List<LimitedSlackLinkDto> workspaces) {
        this.workspaces = workspaces;
    }

    public List<LimitedSlackLinkDto> getWorkspaces() {
        return workspaces;
    }
}
