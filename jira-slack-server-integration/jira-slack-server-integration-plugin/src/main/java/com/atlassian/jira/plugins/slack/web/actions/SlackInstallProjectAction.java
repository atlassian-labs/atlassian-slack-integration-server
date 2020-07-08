package com.atlassian.jira.plugins.slack.web.actions;

import com.atlassian.jira.plugins.slack.manager.ProjectConfigurationManager;
import com.atlassian.jira.projectconfig.util.ProjectConfigRequestCache;
import com.atlassian.jira.util.velocity.VelocityRequestContextFactory;
import com.atlassian.jira.web.action.ActionViewDataMappings;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.plugins.slack.spi.SlackRoutesProviderFactory;
import com.atlassian.plugins.slack.user.SlackUserManager;
import com.atlassian.webresource.api.assembler.PageBuilderService;
import com.google.common.collect.ImmutableMap;
import webwork.action.ServletActionContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Map;

import static com.atlassian.jira.plugins.slack.spi.impl.JiraConfigurationRedirectionManager.FROM_PROJECT_ATTRIBUTE_KEY;
import static com.atlassian.jira.plugins.slack.spi.impl.JiraConfigurationRedirectionManager.PROJECT_ATTRIBUTE_KEY;

public class SlackInstallProjectAction extends AbstractProjectAction {
    private static final String CONTEXT_ATTRIBUTE_LABEL = "context";

    public SlackInstallProjectAction(final VelocityRequestContextFactory contextFactory,
                                     final PageBuilderService pageBuilderService,
                                     final ProjectConfigRequestCache projectConfigRequestCache,
                                     final ProjectConfigurationManager projectConfigurationManager,
                                     final SlackUserManager slackUserManager,
                                     final SlackRoutesProviderFactory slackRoutesProviderFactory,
                                     final SlackClientProvider slackClientProvider,
                                     final SlackLinkManager slackLinkManager) {
        super(contextFactory, pageBuilderService, projectConfigRequestCache, projectConfigurationManager,
                slackUserManager, slackRoutesProviderFactory, slackClientProvider, slackLinkManager);
    }

    @Override
    protected String doExecute() throws Exception {
        String superResult = super.doExecute();
        if (!SUCCESS.equals(superResult)) {
            return superResult;
        }

        HttpSession session = ServletActionContext.getRequest().getSession();
        Map<String, Object> contextFromSession = (Map<String, Object>) session.getAttribute(CONTEXT_ATTRIBUTE_LABEL);
        if (contextFromSession != null) {
            session.removeAttribute(CONTEXT_ATTRIBUTE_LABEL);
        }
        session.setAttribute(FROM_PROJECT_ATTRIBUTE_KEY, true);
        session.setAttribute(PROJECT_ATTRIBUTE_KEY, projectKey);

        return SUCCESS;
    }

    @ActionViewDataMappings({SUCCESS})
    public Map<String, Object> getDataMap() {
        return ImmutableMap.<String, Object>builder()
                .put(PROJECT_KEY, getProjectKey())
                .build();
    }

    protected String getRequestUrl(HttpServletRequest request) {
        return "/secure/InstallSlack.jspa?" + request.getQueryString();
    }
}
