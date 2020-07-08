package com.atlassian.jira.plugins.slack.util;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.util.JiraVelocityHelper;
import org.springframework.stereotype.Component;

@Component
public class JiraVelocityHelperHolder {
    private final JiraVelocityHelper velocityHelper;

    public JiraVelocityHelperHolder() {
        this.velocityHelper = new JiraVelocityHelper(ComponentAccessor.getFieldManager());
    }

    public JiraVelocityHelper getVelocityHelper() {
        return velocityHelper;
    }
}
