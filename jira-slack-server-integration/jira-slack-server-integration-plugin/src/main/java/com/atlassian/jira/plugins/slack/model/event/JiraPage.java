package com.atlassian.jira.plugins.slack.model.event;

import com.atlassian.plugins.slack.api.events.VisitablePage;

public enum JiraPage implements VisitablePage {
    PROJECT_CONFIG("project.config");

    private final String suffix;

    JiraPage(final String suffix) {
        this.suffix = suffix;
    }

    @Override
    public String getSuffix() {
        return suffix;
    }
}
