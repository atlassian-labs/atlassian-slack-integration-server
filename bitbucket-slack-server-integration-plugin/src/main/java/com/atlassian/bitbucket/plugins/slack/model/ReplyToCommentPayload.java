package com.atlassian.bitbucket.plugins.slack.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ReplyToCommentPayload {
    public static final String TYPE_PULL_REQUEST = "pullRequest";
    public static final String TYPE_COMMIT = "commit";

    private final String objectType;
    private final String objectId;
    private final int repositoryId;
    private final long commentId;

    @JsonCreator
    public ReplyToCommentPayload(@JsonProperty("objectType") final String objectType,
                                 @JsonProperty("objectId") final String objectId,
                                 @JsonProperty("repositoryId") final int repositoryId,
                                 @JsonProperty("commentId") final long commentId) {
        this.objectType = objectType;
        this.objectId = objectId;
        this.repositoryId = repositoryId;
        this.commentId = commentId;
    }

    public String getObjectType() {
        return objectType;
    }

    public String getObjectId() {
        return objectId;
    }

    public int getRepositoryId() {
        return repositoryId;
    }

    public long getCommentId() {
        return commentId;
    }
}
