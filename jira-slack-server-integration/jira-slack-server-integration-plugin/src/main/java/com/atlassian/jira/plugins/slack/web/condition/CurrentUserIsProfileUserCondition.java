package com.atlassian.jira.plugins.slack.web.condition;

import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;

import java.util.Map;

public class CurrentUserIsProfileUserCondition implements Condition {
    @Override
    public void init(final Map<String, String> params) throws PluginParseException {
    }

    @Override
    public boolean shouldDisplay(final Map<String, Object> context) {
        Object currentUser = context.get("user");
        Object profileUser = context.get("profileUser");
        return currentUser != null && currentUser.equals(profileUser);
    }
}
