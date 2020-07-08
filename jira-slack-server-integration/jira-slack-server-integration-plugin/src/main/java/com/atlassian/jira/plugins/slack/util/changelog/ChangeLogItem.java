package com.atlassian.jira.plugins.slack.util.changelog;

public class ChangeLogItem {
    private final String field;
    private final String fieldName;
    private final String newText;
    private final String newValue;
    private final String oldText;
    private final String oldValue;

    ChangeLogItem(String field, String fieldName, String newText, String newValue, String oldText, String oldValue) {
        this.field = field;
        this.fieldName = fieldName;
        this.newText = newText;
        this.newValue = newValue;
        this.oldText = oldText;
        this.oldValue = oldValue;
    }

    public String getField() {
        return field;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getNewText() {
        return newText;
    }

    public String getNewValue() {
        return newValue;
    }

    public String getOldText() {
        return oldText;
    }

    public String getOldValue() {
        return oldValue;
    }
}
