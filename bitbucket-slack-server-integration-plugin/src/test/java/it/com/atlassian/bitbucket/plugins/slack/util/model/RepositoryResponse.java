package it.com.atlassian.bitbucket.plugins.slack.util.model;

import lombok.Value;

@Value
public class RepositoryResponse {
    int id;
    String slug;
    String name;
}
