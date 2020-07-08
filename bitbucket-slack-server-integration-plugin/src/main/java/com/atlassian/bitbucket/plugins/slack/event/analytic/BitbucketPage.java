package com.atlassian.bitbucket.plugins.slack.event.analytic;

import com.atlassian.plugins.slack.api.events.VisitablePage;

public enum  BitbucketPage implements VisitablePage {
    REPOSITORY_CONFIG("repository.config");

    private final String suffix;

    BitbucketPage(final String suffix) {
        this.suffix = suffix;
    }

    @Override
    public String getSuffix() {
        return suffix;
    }
}
