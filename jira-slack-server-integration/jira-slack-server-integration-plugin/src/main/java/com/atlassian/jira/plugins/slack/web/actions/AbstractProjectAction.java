package com.atlassian.jira.plugins.slack.web.actions;

import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.plugins.slack.manager.ProjectConfigurationManager;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.projectconfig.util.ProjectConfigRequestCache;
import com.atlassian.jira.util.velocity.VelocityRequestContext;
import com.atlassian.jira.util.velocity.VelocityRequestContextFactory;
import com.atlassian.jira.util.velocity.VelocityRequestSession;
import com.atlassian.jira.web.ExecutingHttpRequest;
import com.atlassian.jira.web.SessionKeys;
import com.atlassian.jira.web.action.ActionViewDataMappings;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.SlackUser;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.atlassian.plugins.slack.api.routes.SlackRoutesProvider;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.plugins.slack.spi.SlackRoutesProviderFactory;
import com.atlassian.plugins.slack.user.SlackUserManager;
import com.atlassian.seraph.util.RedirectUtils;
import com.atlassian.webresource.api.assembler.PageBuilderService;
import com.github.seratch.jslack.api.model.User;
import com.google.common.collect.ImmutableMap;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings("WeakerAccess")
@RequiredArgsConstructor
public abstract class AbstractProjectAction extends JiraWebActionSupport {
    protected static final String PROJECT_KEY = "projectKey";
    protected static final String ERRORS = "errors";

    protected final VelocityRequestContextFactory contextFactory;
    protected final PageBuilderService pageBuilderService;
    protected final ProjectConfigRequestCache projectConfigRequestCache;
    protected final ProjectConfigurationManager projectConfigurationManager;
    protected final SlackUserManager slackUserManager;
    protected final SlackRoutesProviderFactory slackRoutesProviderFactory;
    protected final SlackClientProvider slackClientProvider;
    protected final SlackLinkManager slackLinkManager;

    protected Project project;
    protected String projectKey;

    private SlackRoutesProvider slackRoutesProvider;

    @Override
    protected String doExecute() throws Exception {
        if (getProject() == null || !hasProjectAdminPermission()) {
            if (getLoggedInUser() == null) {
                final HttpServletRequest request = ExecutingHttpRequest.get();
                return forceRedirect(RedirectUtils.getLoginUrl(request));
            } else {
                return "securitybreach";
            }
        }

        requireResources();
        initRequest();
        setBackToProjectPath();

        return SUCCESS;
    }

    /**
     * You can administer a project if you have explicit PROJECT_ADMIN permission on that project, or are an administrator
     */
    private boolean hasProjectAdminPermission() {
        return hasProjectPermission(ProjectPermissions.ADMINISTER_PROJECTS, getProject()) || getAuthorizationSupport().hasGlobalPermission(GlobalPermissionKey.ADMINISTER);
    }

    @ActionViewDataMappings({ERROR})
    public Map<String, Object> getErrorDataMap() {
        return ImmutableMap.<String, Object>builder()
                .put(PROJECT_KEY, getProjectKey())
                .put(ERRORS, getErrorMessages())
                .build();
    }

    private void initRequest() {
        projectConfigRequestCache.setProject(getProject());
    }

    private void requireResources() {
        pageBuilderService.assembler().resources().requireContext("jira.plugin.slack.project.configure");
        // Not present in older versions of Jira, so we require it here to avoid errors in soy templates, see https://jira.atlassian.com/browse/HC-12820
        pageBuilderService.assembler().resources().requireContext("com.atlassian.auiplugin:aui-flag");
    }

    /**
     * Configure the back to project path so that we could point back to this admin page in the header section when user
     * has navigated to other admin pages. see the "return-to-config" web-item in jira-project-config-plugin.
     */
    private void setBackToProjectPath() {
        final VelocityRequestContext requestContext = contextFactory.getJiraVelocityRequestContext();
        final VelocityRequestSession session = requestContext.getSession();

        // set the project key so that com.atlassian.jira.projectconfig.contextproviders.ReturnToConfigContextProvider.shouldDisplay
        // will return true
        session.setAttribute(SessionKeys.CURRENT_ADMIN_PROJECT, getProjectKeyOrEmpty());
        final HttpServletRequest req = ExecutingHttpRequest.get();
        session.setAttribute(SessionKeys.CURRENT_ADMIN_PROJECT_RETURN_URL, getRequestUrl(req));
        req.setAttribute("com.atlassian.jira.projectconfig.util.ServletRequestProjectConfigRequestCache:project",
                getProject());
    }

    abstract protected String getRequestUrl(HttpServletRequest request);

    private String getProjectKeyOrEmpty() {
        Project project = getProject();
        return StringUtils.defaultString(project == null ? null : project.getKey());
    }

    public Project getProject() {
        if (project == null) {
            project = getProjectManager().getProjectObjByKey(getProjectKey());
        }
        return project;
    }

    public String getProjectKey() {
        return projectKey;
    }

    List<SlackLink> getLinks() {
        return slackLinkManager.getLinks();
    }

    Optional<SlackLink> getLink() {
        Optional<SlackLink> result = Optional.ofNullable(getHttpRequest().getParameter("teamId"))
                .flatMap(teamId -> slackLinkManager.getLinkByTeamId(teamId).fold(
                        e -> Optional.empty(),
                        Optional::of
                ));
        if (result.isPresent()) {
            return result;
        }
        return getLinks().stream().findFirst();
    }

    public void setProjectKey(String projectKey) {
        this.projectKey = projectKey;
    }

    Optional<String> getSlackUserName(@Nullable SlackLink link) {
        return Optional.ofNullable(link)
                .flatMap(l -> slackUserManager.getByTeamIdAndUserKey(l.getTeamId(), getLoggedInUser().getKey())
                        .map(slackUser -> slackClientProvider.withLink(l).getUserInfo(slackUser.getSlackUserId())
                                .fold(e -> slackUser.getSlackUserId(), User::getRealName)));
    }

    Optional<String> getSlackUserId(@Nullable SlackLink link) {
        if (link != null) {
            return slackUserManager.getByTeamIdAndUserKey(link.getTeamId(), getLoggedInUser().getKey())
                    .map(SlackUser::getSlackUserId);
        }
        return Optional.empty();
    }

    protected SlackRoutesProvider getSlackRoutesProvider() {
        if (slackRoutesProvider == null) {
            slackRoutesProvider = slackRoutesProviderFactory.getProvider(ImmutableMap.of(
                    "projectKey", getProjectKey()
            ));
        }
        return slackRoutesProvider;
    }
}
