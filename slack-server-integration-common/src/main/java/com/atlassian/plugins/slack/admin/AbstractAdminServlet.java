package com.atlassian.plugins.slack.admin;

import com.atlassian.plugins.slack.spi.SlackLinkAccessManager;
import com.atlassian.sal.api.user.UserManager;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Qualifier;

public abstract class AbstractAdminServlet extends AbstractPermissionCheckingServlet {
    private final SlackLinkAccessManager slackLinkAccessManager;

    AbstractAdminServlet(
            @Qualifier("salUserManager") final UserManager userManager,
            final LoginRedirectionManager loginRedirectionManager,
            final SlackLinkAccessManager slackLinkAccessManager) {
        super(userManager, loginRedirectionManager);
        this.slackLinkAccessManager = slackLinkAccessManager;
    }

    protected boolean checkAccess(final HttpServletRequest request) {
        return slackLinkAccessManager.hasAccess(userManager.getRemoteUser(), request);
    }
}
