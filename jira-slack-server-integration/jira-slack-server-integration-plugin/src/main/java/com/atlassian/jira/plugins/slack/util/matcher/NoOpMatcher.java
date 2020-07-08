package com.atlassian.jira.plugins.slack.util.matcher;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugins.slack.model.ProjectConfiguration;

/**
 * Returns always true, and we keep only one class of this to avoid
 * doing new instantiations
 */
public final class NoOpMatcher implements EventMatcher {
    private static final NoOpMatcher INSTANCE = new NoOpMatcher();

    private NoOpMatcher() {
    }

    @Override
    public boolean matches(Issue issue, ProjectConfiguration config) {
        return true;
    }

    public static NoOpMatcher get() {
        return INSTANCE;
    }
}
