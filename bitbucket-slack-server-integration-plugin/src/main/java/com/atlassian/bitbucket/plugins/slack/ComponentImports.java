package com.atlassian.bitbucket.plugins.slack;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.bitbucket.avatar.AvatarService;
import com.atlassian.bitbucket.comment.CommentService;
import com.atlassian.bitbucket.commit.CommitService;
import com.atlassian.bitbucket.content.ContentService;
import com.atlassian.bitbucket.nav.NavBuilder;
import com.atlassian.bitbucket.permission.PermissionValidationService;
import com.atlassian.bitbucket.pull.PullRequestService;
import com.atlassian.bitbucket.repository.RefService;
import com.atlassian.bitbucket.repository.RepositoryService;
import com.atlassian.bitbucket.scm.ScmService;
import com.atlassian.bitbucket.server.ApplicationPropertiesService;
import com.atlassian.bitbucket.user.SecurityService;
import com.atlassian.bitbucket.user.UserService;
import com.atlassian.bitbucket.watcher.WatcherService;
import com.atlassian.cache.CacheManager;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.PluginController;
import com.atlassian.plugin.module.ModuleFactory;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugin.web.WebInterfaceManager;
import com.atlassian.plugin.webresource.WebResourceUrlProvider;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.executor.ThreadLocalDelegateExecutorFactory;
import com.atlassian.sal.api.message.I18nResolver;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.timezone.TimeZoneManager;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.usersettings.UserSettingsService;
import com.atlassian.sal.api.web.context.HttpContext;
import com.atlassian.soy.renderer.SoyTemplateRenderer;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.atlassian.webresource.api.assembler.PageBuilderService;

@SuppressWarnings("unused")
public class ComponentImports {
    @ComponentImport
    PluginController pluginController;
    @ComponentImport
    WebResourceUrlProvider urlProvider;
    @ComponentImport
    SoyTemplateRenderer soyTemplateRenderer;
    @ComponentImport
    PluginSettingsFactory pluginSettings;
    @ComponentImport
    UserSettingsService userSettingsService;
    @ComponentImport
    EventPublisher eventPublisher;
    @ComponentImport
    I18nResolver i18nResolver;
    @ComponentImport
    ActiveObjects ao;
    @ComponentImport("salUserManager")
    UserManager userManager;
    @ComponentImport("salApplicationProperties")
    ApplicationProperties applicationProperties;
    @ComponentImport
    PluginAccessor pluginAccessor;
    @ComponentImport
    ModuleFactory moduleFactory;
    @ComponentImport
    TransactionTemplate transactionTemplate;
    @ComponentImport
    CacheManager cacheManager;
    @ComponentImport
    PageBuilderService pageBuilderService;
    @ComponentImport
    LoginUriProvider loginUriProvider;
    @ComponentImport
    HttpContext httpContext;
    @ComponentImport
    WebInterfaceManager webInterfaceManager;
    @ComponentImport
    TimeZoneManager timeZoneManager;
    @ComponentImport
    ThreadLocalDelegateExecutorFactory threadLocalDelegateExecutorFactory;
    @ComponentImport
    TemplateRenderer templateRenderer;
    @ComponentImport
    NavBuilder navBuilder;
    @ComponentImport
    ApplicationPropertiesService applicationPropertiesService;
    @ComponentImport
    RepositoryService repositoryService;
    @ComponentImport
    PermissionValidationService permissionValidationService;
    @ComponentImport
    CommitService commitService;
    @ComponentImport
    SecurityService securityService;
    @ComponentImport
    CommentService commentService;
    @ComponentImport
    UserService userService;
    @ComponentImport
    PullRequestService pullRequestService;
    @ComponentImport
    ScmService scmService;
    @ComponentImport
    RefService refService;
    @ComponentImport
    AvatarService avatarService;
    @ComponentImport
    ContentService contentService;
    @ComponentImport
    WatcherService watcherService;
}
