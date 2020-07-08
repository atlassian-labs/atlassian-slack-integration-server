package com.atlassian.confluence.plugins.slack.spacetochannel.events.analytic;

import com.atlassian.plugins.slack.api.events.VisitablePage;

public enum ConfluencePage implements VisitablePage {
    SPACE_CONFIG("space.config");

    private final String suffix;

    ConfluencePage(final String suffix) {
        this.suffix = suffix;
    }

    @Override
    public String getSuffix() {
        return suffix;
    }
}
