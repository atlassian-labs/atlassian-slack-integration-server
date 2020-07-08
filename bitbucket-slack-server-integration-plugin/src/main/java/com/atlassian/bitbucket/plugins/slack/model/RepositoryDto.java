package com.atlassian.bitbucket.plugins.slack.model;

import lombok.Value;
import org.codehaus.jackson.annotate.JsonAutoDetect;

import static org.codehaus.jackson.annotate.JsonAutoDetect.Visibility.PUBLIC_ONLY;

@JsonAutoDetect(fieldVisibility = PUBLIC_ONLY)
@Value
public class RepositoryDto {
    int id;
    String slug;
    String projectKey;
    String name;
    String projectName;
}
