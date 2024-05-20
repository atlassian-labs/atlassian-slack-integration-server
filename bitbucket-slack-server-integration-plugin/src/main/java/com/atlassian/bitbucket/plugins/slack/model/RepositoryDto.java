package com.atlassian.bitbucket.plugins.slack.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.Value;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;

@JsonAutoDetect(fieldVisibility = PUBLIC_ONLY)
@Value
public class RepositoryDto {
    int id;
    String slug;
    String projectKey;
    String name;
    String projectName;
}
