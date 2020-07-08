package com.atlassian.jira.plugins.slack.util.changelog;

import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.plugins.slack.util.JiraVelocityHelperHolder;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.JiraVelocityHelper;
import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;
import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.GenericValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class ChangeLogExtractor {
    private static final Logger log = LoggerFactory.getLogger(ChangeLogExtractor.class);

    public static final String STATUS_FIELD_NAME = "status";
    public static final String ASSIGNEE_FIELD_NAME = "assignee";

    private static final String COMMENT_FIELD_NAME = "Comment";
    private static final String STRING_KEY = "newstring";
    private static final String OLD_STRING_KEY = "oldstring";
    private static final String STRING_KEY_FOR_COMMENT = "newvalue";
    private static final String OLD_STRING_KEY_FOR_COMMENT = "oldvalue";
    private static final String NEW_VALUE = "newvalue";
    private static final String OLD_VALUE = "oldvalue";
    private static final String FIELD_KEY = "field";

    private final JiraVelocityHelper velocityHelper;
    private final I18nHelper i18nHelper;

    @Autowired
    public ChangeLogExtractor(final JiraVelocityHelperHolder velocityHelperHolder,
                              final JiraAuthenticationContext jiraAuthenticationContext) {
        this.velocityHelper = velocityHelperHolder.getVelocityHelper();
        this.i18nHelper = jiraAuthenticationContext.getI18nHelper();
    }

    public List<ChangeLogItem> getChanges(IssueEvent issueEvent) {
        return getChanges(issueEvent, -1);
    }

    public List<ChangeLogItem> getChanges(IssueEvent issueEvent, int maxValueLength) {
        try {
            final GenericValue changeLog = issueEvent.getChangeLog();
            if (changeLog != null) {
                final List<GenericValue> changeItems = changeLog.getRelated("ChildChangeItem");

                final List<ChangeLogItem> changeLogItems = new ArrayList<>(changeItems.size());
                for (GenericValue changeItem : changeItems) {
                    final String field = changeItem.getString(FIELD_KEY);
                    String newStringKey = COMMENT_FIELD_NAME.equals(field) ? STRING_KEY_FOR_COMMENT : STRING_KEY;
                    String fieldName = velocityHelper.getFieldName(changeItem, i18nHelper);
                    String newText = velocityHelper.getPrettyFieldString(field,
                            changeItem.getString(newStringKey),
                            i18nHelper,
                            "");
                    String newValue = changeItem.getString(NEW_VALUE);

                    String oldStringKey = COMMENT_FIELD_NAME.equals(field) ? OLD_STRING_KEY_FOR_COMMENT : OLD_STRING_KEY;
                    String oldText = velocityHelper.getPrettyFieldString(field,
                            changeItem.getString(oldStringKey),
                            i18nHelper,
                            "");
                    String oldValue = changeItem.getString(OLD_VALUE);

                    if (Strings.isNullOrEmpty(newText) && field.equals(ASSIGNEE_FIELD_NAME)) {
                        newText = i18nHelper.getText("common.concepts.unassigned");
                    }

                    if (!Strings.isNullOrEmpty(newText)) {
                        if (maxValueLength != -1) {
                            newText = StringUtils.abbreviate(newText, maxValueLength);
                        }
                        changeLogItems.add(new ChangeLogItem(field, fieldName, newText, newValue, oldText, oldValue));
                    }
                }

                return changeLogItems;
            }


        } catch (GenericEntityException e) {
            log.error("Failed to get changes from changelog", e);
        }

        return Collections.emptyList();
    }
}
