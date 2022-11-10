package com.atlassian.confluence.plugins.slack.spacetochannel.notifications;

public enum ConfluencePersonalNotificationTypes {
    /**
     * Tell me about updates in pages or posts I'm watching
     */
    WATCHER_UPDATES,
    /**
     * Tell me about comments in pages or posts I'm watching
     */
    WATCHER_COMMENTS,
    /**
     * Tell me about updates on pages or posts I created
     */
    CREATOR_UPDATES,
    /**
     * Tell me about comments on pages or posts I created
     */
    CREATOR_COMMENTS
}
