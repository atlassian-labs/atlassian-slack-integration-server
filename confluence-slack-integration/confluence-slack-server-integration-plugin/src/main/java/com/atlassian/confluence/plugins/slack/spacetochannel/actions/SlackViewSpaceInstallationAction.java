package com.atlassian.confluence.plugins.slack.spacetochannel.actions;

import com.atlassian.confluence.spaces.actions.AbstractSpaceAdminAction;
import com.atlassian.xwork.PermittedMethods;
import com.opensymphony.xwork.Action;

import javax.servlet.http.HttpSession;
import java.util.Map;

import static com.atlassian.confluence.plugins.slack.spi.impl.ConfluenceConfigurationRedirectionManager.FROM_SPACE_ATTRIBUTE_KEY;
import static com.atlassian.confluence.plugins.slack.spi.impl.ConfluenceConfigurationRedirectionManager.SPACE_ATTRIBUTE_KEY;
import static com.atlassian.xwork.HttpMethod.GET;

public class SlackViewSpaceInstallationAction extends AbstractSpaceAdminAction {
    static final String CONTEXT_ATTRIBUTE_LABEL = "context";

    @PermittedMethods(GET)
    @Override
    public String execute() {
        HttpSession session = getCurrentRequest().getSession();
        @SuppressWarnings("unchecked")
        final Map<String, Object> contextFromSession = (Map<String, Object>) session.getAttribute(CONTEXT_ATTRIBUTE_LABEL);
        if (contextFromSession != null) {
            session.removeAttribute(CONTEXT_ATTRIBUTE_LABEL);
        }

        session.setAttribute(FROM_SPACE_ATTRIBUTE_KEY, true);
        session.setAttribute(SPACE_ATTRIBUTE_KEY, getSpaceKey());
        return Action.SUCCESS;
    }

}
