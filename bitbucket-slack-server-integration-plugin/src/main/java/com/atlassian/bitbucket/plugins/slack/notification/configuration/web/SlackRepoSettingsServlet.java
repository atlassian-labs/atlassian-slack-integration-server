package com.atlassian.bitbucket.plugins.slack.notification.configuration.web;

import com.atlassian.bitbucket.permission.Permission;
import com.atlassian.bitbucket.permission.PermissionValidationService;
import com.atlassian.bitbucket.plugins.slack.event.analytic.BitbucketPage;
import com.atlassian.bitbucket.plugins.slack.notification.configuration.NotificationConfigurationContextBuilder;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.repository.RepositoryService;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugins.slack.analytics.AnalyticsContextProvider;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.events.PageVisitedEvent;
import com.atlassian.soy.renderer.SoyException;
import com.atlassian.soy.renderer.SoyTemplateRenderer;
import com.atlassian.webresource.api.assembler.PageBuilderService;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import lombok.RequiredArgsConstructor;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@RequiredArgsConstructor
public class SlackRepoSettingsServlet extends HttpServlet {
    private static final String PROJECTS = "projects";
    private static final String REPOS = "repos";

    private static final String RESOURCE_KEY = "com.atlassian.bitbucket.plugins.bitbucket-slack-server-integration-plugin:slack-soy-server-resources";
    private static final String SETTINGS_PAGE_CONTEXT = "bitbucket.page.repo.slack.settings";
    private static final String TEMPLATE_KEY = "bitbucket.page.slack.repo.config.RepoSettings";

    private final NotificationConfigurationContextBuilder notificationConfigurationContextBuilder;
    private final PageBuilderService pageBuilderService;
    private final PermissionValidationService permissionValidationService;
    private final RepositoryService repositoryService;
    private final SoyTemplateRenderer soyTemplateRenderer;
    private final EventPublisher eventPublisher;
    private final AnalyticsContextProvider analyticsContextProvider;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final String teamId = req.getParameter("teamId");
        Repository repository = getRepository(req);
        if (repository == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        permissionValidationService.validateForRepository(repository, Permission.REPO_ADMIN);
        prepareResponse(resp);
        Map<String, Object> viewContext = createViewContext(teamId, repository);
        eventPublisher.publish(new PageVisitedEvent(analyticsContextProvider.bySlackLink((SlackLink) viewContext.get("link")),
                BitbucketPage.REPOSITORY_CONFIG));

        render(resp, viewContext);
    }

    private Map<String, Object> createViewContext(final String teamId, final Repository repository) {
        ImmutableMap.Builder<String, Object> contextBuilder =
                notificationConfigurationContextBuilder.createRepositoryViewContext(teamId, repository);
        return contextBuilder.build();
    }

    Repository getRepository(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        if (Strings.isNullOrEmpty(pathInfo) || pathInfo.equals("/")) {
            return null;
        }
        String[] pathParts = pathInfo.substring(1).split("/");
        if (!isRepoPath(pathParts)) {
            return null;
        }
        String projectKey = pathParts[1];
        String repoSlug = pathParts[3];
        return repositoryService.getBySlug(projectKey, repoSlug);
    }

    private void handleSoyError(SoyException e) throws IOException, ServletException {
        Throwable cause = e.getCause();
        if (cause instanceof IOException) {
            throw (IOException) cause;
        }
        throw new ServletException(e);
    }

    private boolean isRepoPath(String[] pathParts) {
        return pathParts.length == 4 && PROJECTS.equals(pathParts[0]) && REPOS.equals(pathParts[2]);
    }

    private void prepareResponse(HttpServletResponse response) {
        pageBuilderService.assembler().resources().requireContext(SETTINGS_PAGE_CONTEXT);
        response.setContentType("text/html;charset=UTF-8");
    }

    private void render(HttpServletResponse response, Map<String, Object> context) throws ServletException, IOException {
        try {
            soyTemplateRenderer.render(response.getWriter(), RESOURCE_KEY, TEMPLATE_KEY, context);
        } catch (SoyException soyException) {
            handleSoyError(soyException);
        }
    }
}
