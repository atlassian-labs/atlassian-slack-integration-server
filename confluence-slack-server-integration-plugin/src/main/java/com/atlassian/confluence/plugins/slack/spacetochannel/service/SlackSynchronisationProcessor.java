package com.atlassian.confluence.plugins.slack.spacetochannel.service;

public interface SlackSynchronisationProcessor {

    /**
     * This method is initiating a Slack glance synchronisation request.
     */
    void initiateSynchronisation();

}
