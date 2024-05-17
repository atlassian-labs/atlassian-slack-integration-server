package com.atlassian.confluence.plugins.slack;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.cache.CacheManager;
import com.atlassian.confluence.api.service.search.CQLSearchService;
import com.atlassian.confluence.content.CustomContentManager;
import com.atlassian.confluence.core.ContentPermissionManager;
import com.atlassian.confluence.languages.LocaleManager;
import com.atlassian.confluence.mail.notification.NotificationManager;
import com.atlassian.confluence.pages.PageManager;
import com.atlassian.confluence.persistence.EntityManagerProvider;
import com.atlassian.confluence.search.v2.SearchManager;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.security.SpacePermissionManager;
import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.confluence.spaces.SpaceLogoManager;
import com.atlassian.confluence.spaces.SpaceManager;
import com.atlassian.confluence.status.service.SystemInformationService;
import com.atlassian.confluence.user.PersonalInformationManager;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.confluence.util.i18n.I18NBeanFactory;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.PluginController;
import com.atlassian.plugin.module.ModuleFactory;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugin.web.WebInterfaceManager;
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
import com.atlassian.webresource.api.WebResourceUrlProvider;
import com.atlassian.webresource.api.assembler.PageBuilderService;

@SuppressWarnings("unused")
public class ComponentImports {
    @ComponentImport
    LocaleManager localeManager;
    @ComponentImport
    SpacePermissionManager spacePermissionManager;
    @ComponentImport
    PluginController pluginController;
    @ComponentImport
    WebResourceUrlProvider urlProvider;
    @ComponentImport
    TemplateRenderer renderer;
    @ComponentImport
    SoyTemplateRenderer soyTemplateRenderer;
    @ComponentImport
    PluginSettingsFactory pluginSettings;
    @ComponentImport
    UserSettingsService userSettingsService;
    @ComponentImport
    UserAccessor userAccessor;
    @ComponentImport
    EventPublisher eventPublisher;
    @ComponentImport
    ContentPermissionManager contentPermissionManager;
    @ComponentImport
    SpaceManager spaceManager;
    @ComponentImport
    SpaceLogoManager spaceLogoManager;
    @ComponentImport
    PermissionManager permissionManager;
    @ComponentImport
    I18NBeanFactory i18NBeanFactory;
    @ComponentImport
    I18nResolver i18nResolver;
    @ComponentImport
    ActiveObjects ao;
    @ComponentImport
    EntityManagerProvider entityManagerProvider;
    @ComponentImport
    SystemInformationService systemInformationService;
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
    CQLSearchService cqlSearchService;
    @ComponentImport
    SearchManager searchManager;
    @ComponentImport
    PageManager pageManager;
    @ComponentImport
    CacheManager cacheManager;
    @ComponentImport
    SettingsManager settingsManager;
    @ComponentImport
    PageBuilderService pageBuilderService;
    @ComponentImport
    PersonalInformationManager personalInformationManager;
    @ComponentImport
    LoginUriProvider loginUriProvider;
    @ComponentImport
    HttpContext httpContext;
    @ComponentImport
    WebInterfaceManager webInterfaceManager;
    @ComponentImport
    TimeZoneManager timeZoneManager;
    @ComponentImport
    CustomContentManager customContentManager;
    @ComponentImport
    ThreadLocalDelegateExecutorFactory threadLocalDelegateExecutorFactory;
    @ComponentImport
    NotificationManager notificationManager;
}
