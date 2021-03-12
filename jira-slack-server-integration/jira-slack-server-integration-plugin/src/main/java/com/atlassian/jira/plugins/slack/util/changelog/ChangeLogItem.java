package com.atlassian.jira.plugins.slack.util.changelog;

import lombok.Value;
import org.apache.commons.lang3.StringUtils;

@Value
public class ChangeLogItem {
    String field;
    String newText;
    String newValue;
    String oldText;
    String oldValue;

    public String getNewTextTruncated(final int maxLength) {
        return StringUtils.abbreviate(newText, maxLength);
    }

    public String getOldTextTruncated(final int maxLength) {
        return StringUtils.abbreviate(oldText, maxLength);
    }
}
