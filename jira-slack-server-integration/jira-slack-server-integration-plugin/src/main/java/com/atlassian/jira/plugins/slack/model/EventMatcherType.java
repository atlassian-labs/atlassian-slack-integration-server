package com.atlassian.jira.plugins.slack.model;

import com.atlassian.jira.event.type.EventType;
import com.atlassian.jira.plugins.slack.util.matcher.EventMatcher;
import com.atlassian.jira.plugins.slack.util.matcher.NoOpMatcher;
import com.atlassian.jira.plugins.slack.util.matcher.WorkflowStatusMatcher;

import java.util.HashMap;
import java.util.Map;

/**
 * This enum encapsulates information that an event can be identified by. This is not the only way events reach us. An
 * example of another way is {SlackPostFunction}. All of the matcher defined here will be
 * displayed in the configuration UI and user/admin has the ability to turn them on/off. Once they are enabled, events
 * should pass through {@link com.atlassian.jira.plugins.slack.model.EventFilterType}s to be considered for sending.
 */
public enum EventMatcherType {

    ISSUE_CREATED("MATCHER:ISSUE_CREATED", NoOpMatcher.get(), EventType.ISSUE_CREATED_ID) {
        @Override
        public <T, E extends Throwable> T accept(Visitor<T, E> visitor) throws E {
            return visitor.visitCreated();
        }
    },

    ISSUE_UPDATED("MATCHER:ISSUE_UPDATED",
            NoOpMatcher.get(),
            EventType.ISSUE_UPDATED_ID,
            EventType.ISSUE_ASSIGNED_ID,
            EventType.ISSUE_WORKLOGGED_ID,
            EventType.ISSUE_WORKLOG_UPDATED_ID,
            EventType.ISSUE_WORKLOG_DELETED_ID) {
        @Override
        public <T, E extends Throwable> T accept(Visitor<T, E> visitor) throws E {
            return visitor.visitUpdated();
        }
    },

    /**
     * We need to add multiple events to the issue transitioned cause Jira classic workflow send specific event types
     * for each transition.
     */
    ISSUE_TRANSITIONED("MATCHER:ISSUE_TRANSITIONED", new WorkflowStatusMatcher()) {
        @Override
        public <T, E extends Throwable> T accept(Visitor<T, E> visitor) throws E {
            return visitor.visitTransitioned();
        }
    },

    ISSUE_COMMENTED("MATCHER:ISSUE_COMMENTED",
            NoOpMatcher.get(),
            EventType.ISSUE_COMMENTED_ID,
            EventType.ISSUE_COMMENT_EDITED_ID) {
        @Override
        public <T, E extends Throwable> T accept(Visitor<T, E> visitor) throws E {
            return visitor.visitCommented();
        }
    },
    ISSUE_ASSIGNMENT_CHANGED("MATCHER:ISSUE_ASSIGNMENT_CHANGED",
            NoOpMatcher.get(),
            EventType.ISSUE_ASSIGNED_ID) {
        @Override
        public <T, E extends Throwable> T accept(Visitor<T, E> visitor) throws E {
            return visitor.visitAssignmentChanged();
        }
    };

    public interface Visitor<T, E extends Throwable> {
        T visitCreated() throws E;

        T visitUpdated() throws E;

        T visitTransitioned() throws E;

        T visitCommented() throws E;

        T visitAssignmentChanged() throws E;
    }


    private static final Map<String, EventMatcherType> dbKeyToEnum = new HashMap<>();
    private static final Map<Long, EventMatcherType> jiraEventIdToEnum = new HashMap<>();

    static {
        for (EventMatcherType e : values()) {

            dbKeyToEnum.put(e.dbKey, e);

            for (Long jiraEventId : e.jiraEventIds) {
                jiraEventIdToEnum.put(jiraEventId, e);
            }
        }
    }

    public static EventMatcherType fromName(String dbKey) {
        return dbKeyToEnum.get(dbKey);
    }

    public static EventMatcherType fromJiraEventId(long jiraEventId) {
        return jiraEventIdToEnum.get(jiraEventId);
    }

    private final String dbKey;
    private final Long[] jiraEventIds;
    private final EventMatcher matcher;

    EventMatcherType(final String dbKey, EventMatcher matcher, final Long... jiraEventId) {
        this.dbKey = dbKey;
        this.jiraEventIds = jiraEventId;
        this.matcher = matcher;
    }

    public String getDbKey() {
        return dbKey;
    }

    public EventMatcher getMatcher() {
        return matcher;
    }

    public abstract <T, E extends Throwable> T accept(Visitor<T, E> visitor) throws E;
}
