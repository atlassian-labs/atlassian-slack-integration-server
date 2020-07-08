package com.atlassian.jira.plugins.slack.model.event;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.user.ApplicationUser;
import com.google.common.annotations.VisibleForTesting;

/**
 * This is the default implementation of {@link com.atlassian.jira.plugins.slack.model.event.JiraPostFunctionEvent}.
 */
public class DefaultJiraPostFunctionEvent implements JiraPostFunctionEvent {
    private final Issue issue;
    private final ApplicationUser actor;
    private final String firstStepName;
    private final String endStepName;
    private final String actionName;
    private final String customMessageFormat;
    private final boolean havingErrors;

    @VisibleForTesting
    public DefaultJiraPostFunctionEvent(Issue issue,
                                        ApplicationUser actor,
                                        String firstStepName,
                                        String endStepName,
                                        String actionName,
                                        String customMessageFormat,
                                        boolean havingErrors) {
        this.issue = issue;
        this.actor = actor;
        this.firstStepName = firstStepName;
        this.endStepName = endStepName;
        this.actionName = actionName;
        this.customMessageFormat = customMessageFormat;
        this.havingErrors = havingErrors;
    }

    // These getter methods are necessary for template rendering.

    @Override
    public Issue getIssue() {
        return issue;
    }

    @Override
    public ApplicationUser getActor() {
        return actor;
    }

    @Override
    public String getFirstStepName() {
        return firstStepName;
    }

    @Override
    public String getEndStepName() {
        return endStepName;
    }

    @Override
    public String getActionName() {
        return actionName;
    }

    @Override
    public String getCustomMessageFormat() {
        return customMessageFormat;
    }

    @Override
    public boolean isHavingErrors() {
        return havingErrors;
    }

    public static class Builder {
        private Issue issue;
        private ApplicationUser actor;
        private String firstStepName;
        private String endStepName;
        private String actionName;
        private String customMessageFormat;
        private boolean havingErrors;

        public Builder setIssue(Issue issue) {
            this.issue = issue;
            return this;
        }

        public Builder setActor(ApplicationUser actor) {
            this.actor = actor;
            return this;
        }

        public Builder setFirstStepName(String firstStepName) {
            this.firstStepName = firstStepName;
            return this;
        }

        public Builder setEndStepName(String endStepName) {
            this.endStepName = endStepName;
            return this;
        }

        public Builder setActionName(String actionName) {
            this.actionName = actionName;
            return this;
        }

        public Builder setCustomMessageFormat(String customMessageFormat) {
            this.customMessageFormat = customMessageFormat;
            return this;
        }

        public Builder setHavingErrors(boolean havingErrors) {
            this.havingErrors = havingErrors;
            return this;
        }

        public DefaultJiraPostFunctionEvent build() {
            return new DefaultJiraPostFunctionEvent(issue, actor, firstStepName, endStepName, actionName,
                    customMessageFormat,
                    havingErrors);
        }
    }
}
