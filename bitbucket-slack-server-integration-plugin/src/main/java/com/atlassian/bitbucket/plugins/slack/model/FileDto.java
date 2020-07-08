package com.atlassian.bitbucket.plugins.slack.model;

import com.atlassian.bitbucket.commit.Commit;
import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.bitbucket.repository.Ref;
import com.atlassian.bitbucket.repository.Repository;
import lombok.Value;

import java.util.List;
import java.util.Optional;

@Value
public class FileDto {
    Repository repository;
    Optional<Ref> ref;
    Optional<PullRequest> pullRequest;
    Commit commit;
    String path;
    List<LineRange> lineRanges;

    public String getName() {
        String[] pathParts = path.split("/");
        return pathParts[pathParts.length - 1];
    }
}
