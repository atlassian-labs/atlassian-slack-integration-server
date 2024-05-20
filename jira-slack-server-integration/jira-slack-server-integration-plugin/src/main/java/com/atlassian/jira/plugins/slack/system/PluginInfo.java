package com.atlassian.jira.plugins.slack.system;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Date;

/**
 * Contains plugin info such as build version and timestamp
 */
public class PluginInfo {
    @JsonProperty
    private String version;

    @JsonSerialize(using = CustomDateTimeSerializer.class)
    private Date buildDate;

    @JsonCreator
    public PluginInfo(String version, Date buildDate) {
        this.version = version;
        this.buildDate = buildDate;
    }

    public String getVersion() {
        return version;
    }

    public Date getBuildDate() {
        return buildDate;
    }
}
