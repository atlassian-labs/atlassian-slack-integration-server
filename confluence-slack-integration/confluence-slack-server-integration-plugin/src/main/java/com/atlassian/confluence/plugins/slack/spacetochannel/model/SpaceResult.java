package com.atlassian.confluence.plugins.slack.spacetochannel.model;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SpaceResult {
    private final String name;
    private final String key;

    @JsonCreator
    public SpaceResult(
            @JsonProperty("name") String name,
            @JsonProperty("key") String key) {
        this.name = name;
        this.key = key;
    }

    @JsonProperty
    public String getName() {
        return name;
    }

    @JsonProperty
    public String getKey() {
        return key;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof SpaceResult)) return false;
        final SpaceResult that = (SpaceResult) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, key);
    }

    @Override
    public String toString() {
        return "SpaceResult{" +
                "name='" + name + '\'' +
                ", key='" + key + '\'' +
                '}';
    }
}
