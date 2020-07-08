package com.atlassian.plugins.slack.admin;

import com.atlassian.sal.api.auth.LoginUriProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;

/**
 * Code borrowed from com.atlassian.analytics.client.LoginPageRedirector, which
 * was in turn borrowed from com.atlassian.upm.PluginManagerHandler
 */
@Component
public class LoginRedirectionManager {
    private final LoginUriProvider loginUriProvider;

    private static final String JIRA_SERAPH_SECURITY_ORIGINAL_URL = "os_security_originalurl";
    private static final String CONF_SERAPH_SECURITY_ORIGINAL_URL = "seraph_originalurl";

    @Autowired
    public LoginRedirectionManager(final LoginUriProvider loginUriProvider) {
        this.loginUriProvider = loginUriProvider;
    }

    public void redirectToLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final URI uri = getUri(request);
        addSessionAttributes(request, uri.toString());
        response.sendRedirect(loginUriProvider.getLoginUri(uri).toString());
    }

    private URI getUri(final HttpServletRequest request) {
        final String applicationPath = URI.create(request.getRequestURI()).getPath();
        final String contextPath = request.getContextPath();

        StringBuilder builder = new StringBuilder(applicationPath.replace(contextPath, ""));
        if (request.getQueryString() != null) {
            builder.append("?");
            builder.append(request.getQueryString());
        }
        return URI.create(builder.toString());
    }

    private void addSessionAttributes(final HttpServletRequest request, final String uriString) {
        request.getSession().setAttribute(JIRA_SERAPH_SECURITY_ORIGINAL_URL, uriString);
        request.getSession().setAttribute(CONF_SERAPH_SECURITY_ORIGINAL_URL, uriString);
    }
}
