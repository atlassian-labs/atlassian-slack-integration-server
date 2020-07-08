package com.atlassian.confluence.plugins.slack.spacetochannel.actions;

import com.atlassian.confluence.spaces.actions.AbstractSpaceAdminAction;
import com.opensymphony.xwork.Action;

import java.util.Map;

import static com.atlassian.confluence.plugins.slack.spi.impl.ConfluenceConfigurationRedirectionManager.FROM_SPACE_ATTRIBUTE_KEY;
import static com.atlassian.confluence.plugins.slack.spi.impl.ConfluenceConfigurationRedirectionManager.SPACE_ATTRIBUTE_KEY;

public class SlackViewSpaceInstallationAction extends AbstractSpaceAdminAction {
    static final String CONTEXT_ATTRIBUTE_LABEL = "context";

    @Override
    public String execute() {
        @SuppressWarnings("unchecked")
        final Map<String, Object> contextFromSession = (Map<String, Object>) getCurrentSession().getAttribute(CONTEXT_ATTRIBUTE_LABEL);
        if (contextFromSession != null) {
            getCurrentSession().removeAttribute(CONTEXT_ATTRIBUTE_LABEL);
        }

        getCurrentSession().setAttribute(FROM_SPACE_ATTRIBUTE_KEY, true);
        getCurrentSession().setAttribute(SPACE_ATTRIBUTE_KEY, getSpaceKey());
        return Action.SUCCESS;
    }

}
