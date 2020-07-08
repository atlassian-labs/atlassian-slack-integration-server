package com.atlassian.jira.plugins.slack.model;

import java.util.HashMap;
import java.util.Map;

/**
 * This enum encapsulate information about filters that are applied to incoming events, once they are picked by {@link
 * com.atlassian.jira.plugins.slack.model.EventMatcherType}s. An event has to pass all the corresponding filters to be
 * considered for sending.
 */
public enum EventFilterType {
    ISSUE_TYPE("FILTER:ISSUE_TYPE"),
    ISSUE_PRIORITY("FILTER:ISSUE_PRIORITY"),
    JQL_QUERY("FILTER:JQL_QUERY");

    private static final Map<String, EventFilterType> dbKeyToEnum = new HashMap<>();

    static {
        for (EventFilterType e : values()) {
            dbKeyToEnum.put(e.dbKey, e);
        }
    }

    private final String dbKey;

    EventFilterType(final String dbKey) {
        this.dbKey = dbKey;
    }

    public String getDbKey() {
        return dbKey;
    }

    public static EventFilterType fromName(String dbKey) {
        return dbKeyToEnum.get(dbKey);
    }
}
