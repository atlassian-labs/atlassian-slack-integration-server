package com.atlassian.jira.plugins.slack.service.listener;

import lombok.Value;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

@Value
public class IssueReference {
    String key;
    String url;

    public boolean hasUrl() {
        return StringUtils.isNotBlank(url);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof IssueReference)) return false;
        final IssueReference that = (IssueReference) o;
        return Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }
}
