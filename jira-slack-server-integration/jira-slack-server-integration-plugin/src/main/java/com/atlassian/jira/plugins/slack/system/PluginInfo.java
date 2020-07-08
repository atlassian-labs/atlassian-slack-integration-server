package com.atlassian.jira.plugins.slack.system;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;

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
