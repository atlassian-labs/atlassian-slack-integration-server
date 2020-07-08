package com.atlassian.jira.plugins.slack.model.event;

import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugins.slack.model.EventMatcherType;
import com.atlassian.jira.user.ApplicationUser;
import com.google.common.annotations.VisibleForTesting;

/**
 * Default implementation of {@link com.atlassian.jira.plugins.slack.model.event.JiraIssueEvent}
 */
public class DefaultJiraIssueEvent implements JiraIssueEvent {
    private final EventMatcherType eventMatcher;
    private final IssueEvent issueEvent;

    @VisibleForTesting
    public DefaultJiraIssueEvent(final EventMatcherType eventMatcher, final IssueEvent issueEvent) {
        this.eventMatcher = eventMatcher;
        this.issueEvent = issueEvent;
    }

    @Override
    public IssueEvent getIssueEvent() {
        return issueEvent;
    }

    @Override
    public EventMatcherType getEventMatcher() {
        return eventMatcher;
    }

    public Issue getIssue() {
        return issueEvent.getIssue();
    }

    public ApplicationUser getActor() {
        return issueEvent.getUser();
    }

    public static class Builder {
        private EventMatcherType eventMatcher;
        private IssueEvent issueEvent;

        public Builder() {
        }

        public Builder setEventMatcher(EventMatcherType eventMatcher) {
            this.eventMatcher = eventMatcher;
            return this;
        }

        public Builder setIssueEvent(IssueEvent issueEvent) {
            this.issueEvent = issueEvent;
            return this;
        }

        public DefaultJiraIssueEvent build() {
            return new DefaultJiraIssueEvent(eventMatcher, issueEvent);
        }
    }
}
