package com.atlassian.jira.plugins.slack.util;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueConstant;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.plugins.slack.service.notification.impl.JiraPostFunctionEventRenderer.CustomFieldWrapper;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectConstant;
import com.atlassian.jira.user.ApplicationUser;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.app.event.ReferenceInsertionEventHandler;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

/**
 * This class helps rendering of custom post function notifications to be sent to Slack. When user defines a
 * reference to common models in Jira, it'll get a nice text version of it rather than the default toString()
 * reference to that class.
 */
public class JsonPrimitiveReferenceInsertionEventHandler implements ReferenceInsertionEventHandler {
    @Override
    public Object referenceInsert(String reference, Object value) {
        if (value != null
                && !(value instanceof CharSequence)
                && !(value instanceof Number)
                && !value.getClass().isPrimitive()) {
            if (value instanceof Issue) {
                return ((Issue) value).getKey();
            } else if (value instanceof Project) {
                return ((Project) value).getName();
            } else if (value instanceof IssueConstant) {
                final IssueConstant issueConstant = (IssueConstant) value;
                return StringUtils.defaultIfBlank(issueConstant.getNameTranslation(), issueConstant.getName());
            } else if (value instanceof ApplicationUser) {
                return ((ApplicationUser) value).getDisplayName();
            } else if (value instanceof ProjectConstant) {
                return ((ProjectConstant) value).getName();
            } else if (value instanceof CustomFieldWrapper) {
                final CustomFieldWrapper customField = (CustomFieldWrapper) value;
                final Object customFieldValue = customField.getValue();
                final String customFieldValueString = customFieldValue != null ? String.valueOf(customFieldValue) : null;
                return defaultIfBlank(customField.getName(), customField.getFieldName()) + ": "
                        + defaultIfBlank(customFieldValueString, "[empty]");
            } else if (value instanceof CustomField) {
                return ((CustomField) value).getName();
            } else if (value instanceof Map) {
                return ((Map<?, ?>) value).keySet().stream().map(String::valueOf).collect(Collectors.joining(","));
            } else if (value instanceof List) {
                return ((List<?>) value).stream().map(String::valueOf).collect(Collectors.joining(","));
            }
            return reference;
        }
        return value;
    }
}
