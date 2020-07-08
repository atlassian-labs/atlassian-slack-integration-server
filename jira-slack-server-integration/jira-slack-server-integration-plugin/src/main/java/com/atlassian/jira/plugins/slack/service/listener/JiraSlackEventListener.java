package com.atlassian.jira.plugins.slack.service.listener;

/**
 * Listens to Jira Issue events and translates them to Tasks which send notifications to Slack
 * <p>
 * At present this has no methods on the interface
 */
public interface JiraSlackEventListener {

    /**
     * Enables the listener
     */
    void enable() throws Exception;

    /**
     * Disables the listener
     */
    void disable() throws Exception;
}
