package com.atlassian.jira.plugins.slack.web.condition;

import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.webresource.spi.condition.AbstractBooleanUrlReadingCondition;

import java.util.Map;


/**
 * Loads a web resource based when user is logged in.
 * Copied from com.atlassian.confluence.plugin.descriptor.web.UserLoggedInUrlReadingCondition for backward compatibility
 * with Confluence 5.3
 */
public class UserLoggedInUrlReadingCondition extends AbstractBooleanUrlReadingCondition {
    private static final String USER_LOGGED_IN_QUERY_PARAM = "user-logged-in";
    private final JiraAuthenticationContext context;

    public UserLoggedInUrlReadingCondition(JiraAuthenticationContext context) {
        this.context = context;
    }


    @Override
    public void init(Map<String, String> params) throws PluginParseException {
        // noop
    }

    @Override
    protected boolean isConditionTrue() {
        return context.isLoggedInUser();
    }

    @Override
    protected String queryKey() {
        return USER_LOGGED_IN_QUERY_PARAM;
    }

}
