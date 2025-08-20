package com.atlassian.jira.plugins.slack.service;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.cache.CacheManager;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.avatar.AvatarService;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.bc.project.ProjectService;
import com.atlassian.jira.bc.workflow.WorkflowService;
import com.atlassian.jira.config.IssueTypeManager;
import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.entity.property.JsonEntityPropertyManager;
import com.atlassian.jira.help.HelpUrls;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.index.IssueIndexingService;
import com.atlassian.jira.issue.watchers.WatcherManager;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.projectconfig.util.ProjectConfigRequestCache;
import com.atlassian.jira.search.issue.IssueDocumentSearchService;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.UserProjectHistoryManager;
import com.atlassian.jira.user.flag.FlagDismissalService;
import com.atlassian.jira.util.BuildUtilsInfo;
import com.atlassian.jira.util.VelocityParamFactory;
import com.atlassian.jira.util.thread.JiraThreadLocalUtil;
import com.atlassian.jira.util.velocity.VelocityRequestContextFactory;
import com.atlassian.jira.workflow.WorkflowManager;
import com.atlassian.jira.workflow.WorkflowSchemeManager;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugin.web.WebInterfaceManager;
import com.atlassian.plugin.webresource.WebResourceIntegration;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.executor.ThreadLocalDelegateExecutorFactory;
import com.atlassian.sal.api.message.I18nResolver;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.atlassian.sal.api.usersettings.UserSettingsService;
import com.atlassian.sal.api.web.context.HttpContext;
import com.atlassian.soy.renderer.SoyTemplateRenderer;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.atlassian.webresource.api.WebResourceUrlProvider;
import com.atlassian.webresource.api.assembler.PageBuilderService;

@SuppressWarnings("unused")
public class ComponentImports {
    @ComponentImport("jiraUserManager")
    com.atlassian.jira.user.util.UserManager jiraUserManager;
    @ComponentImport("salUserManager")
    com.atlassian.sal.api.user.UserManager userManager;
    @ComponentImport("salApplicationProperties")
    ApplicationProperties applicationProperties;
    @ComponentImport("jiraApplicationProperties")
    com.atlassian.jira.config.properties.ApplicationProperties jiraApplicationProperties;
    @ComponentImport
    I18nResolver i18nResolver;
    @ComponentImport
    IssueDocumentSearchService issueDocumentSearchService;
    @ComponentImport
    BuildUtilsInfo buildUtilsInfo;
    @ComponentImport
    CacheManager cacheManager;
    @ComponentImport
    ActiveObjects ao;
    @ComponentImport
    SearchService searchService;
    @ComponentImport
    PluginSettingsFactory pluginSettings;
    @ComponentImport
    AvatarService avatarService;
    @ComponentImport
    IssueManager issueManager;
    @ComponentImport
    WatcherManager watcherManager;
    @ComponentImport
    CommentManager commentManager;
    @ComponentImport
    JiraAuthenticationContext jiraAuthenticationContext;
    @ComponentImport
    PermissionManager permissionManager;
    @ComponentImport
    EventPublisher eventPublisher;
    JiraAuthenticationContext authenticationContext;
    @ComponentImport
    TemplateRenderer renderer;
    @ComponentImport
    ProjectService projectService;
    @ComponentImport
    VelocityParamFactory velocityParamFactory;
    @ComponentImport
    WorkflowSchemeManager workflowSchemeManager;
    @ComponentImport
    IssueTypeManager issueTypeManager;
    @ComponentImport
    WorkflowManager workflowManager;
    @ComponentImport
    GlobalPermissionManager globalPermissionManager;
    @ComponentImport
    ProjectManager projectManager;
    @ComponentImport
    WorkflowService workflowService;
    @ComponentImport
    FieldManager fieldManager;
    @ComponentImport
    UserProjectHistoryManager userProjectHistoryManager;
    @ComponentImport
    HelpUrls helpUrls;
    @ComponentImport
    VelocityRequestContextFactory contextFactory;
    @ComponentImport
    PageBuilderService pageBuilderService;
    @ComponentImport
    ProjectConfigRequestCache projectConfigRequestCache;
    @ComponentImport("pluginAccessor")
    PluginAccessor pluginAccessor;
    @ComponentImport
    WebResourceIntegration webResourceIntegration;
    @ComponentImport
    DateTimeFormatter dateTimeFormatter;
    @ComponentImport
    WebResourceUrlProvider urlProvider;
    @ComponentImport
    JiraThreadLocalUtil threadLocalUtil;
    @ComponentImport
    JsonEntityPropertyManager jsonEntityPropertyManager;
    @ComponentImport
    WebInterfaceManager webInterfaceManager;
    @ComponentImport
    LoginUriProvider loginUriProvider;
    @ComponentImport
    UserSettingsService userSettingsService;
    @ComponentImport
    SoyTemplateRenderer soyTemplateRenderer;
    @ComponentImport
    TransactionTemplate transactionTemplate;
    @ComponentImport
    HttpContext httpContext;
    @ComponentImport
    IssueIndexingService issueIndexingService;
    @ComponentImport
    ThreadLocalDelegateExecutorFactory threadLocalDelegateExecutorFactory;
    @ComponentImport
    FlagDismissalService flagDismissalService;
    @ComponentImport
    CustomFieldManager customFieldManager;
}
