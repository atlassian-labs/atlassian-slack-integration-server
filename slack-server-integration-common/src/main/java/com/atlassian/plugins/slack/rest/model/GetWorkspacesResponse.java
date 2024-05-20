package com.atlassian.plugins.slack.rest.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;

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
