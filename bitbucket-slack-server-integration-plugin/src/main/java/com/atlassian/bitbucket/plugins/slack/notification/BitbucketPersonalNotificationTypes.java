package com.atlassian.bitbucket.plugins.slack.notification;

public enum BitbucketPersonalNotificationTypes {
    /**
     * Tell me about comments in my commits
     */
    COMMIT_AUTHOR_COMMENT,
    /**
     * Tell me about all updates in my PRs
     */
    PR_AUTHOR,
    /**
     * Tell me about all updates in a PR I’m watching
     */
    PR_WATCHER,
    /**
     * Tell me about new PRs in which I’m a reviewer or if someone adds me as a reviewer
     */
    PR_REVIEWER_CREATED,
    /**
     * Tell me about all updates in a PR I’m a reviewer
     */
    PR_REVIEWER_UPDATED
}
