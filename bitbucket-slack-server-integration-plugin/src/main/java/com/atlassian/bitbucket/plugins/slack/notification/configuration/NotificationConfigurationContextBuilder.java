package com.atlassian.bitbucket.plugins.slack.notification.configuration;

import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.server.ApplicationPropertiesService;
import com.atlassian.bitbucket.util.Page;
import com.atlassian.bitbucket.util.PageRequest;
import com.atlassian.bitbucket.util.PageUtils;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.client.SlackClient;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.plugins.slack.spi.SlackRoutesProviderFactory;
import com.atlassian.plugins.slack.user.SlackUserManager;
import com.atlassian.sal.api.user.UserKey;
import com.atlassian.sal.api.user.UserManager;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class NotificationConfigurationContextBuilder {
    private static final String PLUGIN_PREFIX = "plugin.bitbucket-slack-integration.";
    private static final String PROPERTY_PAGE_SIZE_LIMIT = PLUGIN_PREFIX + "slack.config.page-size";

    private final int pageSizeLimit;

    private final SlackLinkManager slackLinkManager;
    private final SlackUserManager slackUserManager;
    private final SlackClientProvider slackClientProvider;
    private final NotificationConfigurationService notificationConfigService;
    private final UserManager userManager;
    private final SlackRoutesProviderFactory slackRoutesProviderFactory;

    @Autowired
    public NotificationConfigurationContextBuilder(
            final SlackLinkManager slackLinkManager,
            final SlackUserManager slackUserManager,
            final SlackClientProvider slackClientProvider,
            final NotificationConfigurationService notificationConfigService,
            final ApplicationPropertiesService propertiesService,
            final UserManager userManager,
            final SlackRoutesProviderFactory slackRoutesProviderFactory) {
        this.slackClientProvider = slackClientProvider;
        this.slackLinkManager = slackLinkManager;
        this.slackUserManager = slackUserManager;
        this.notificationConfigService = notificationConfigService;
        this.userManager = userManager;

        this.pageSizeLimit = propertiesService.getPluginProperty(PROPERTY_PAGE_SIZE_LIMIT, PageRequest.MAX_PAGE_LIMIT);
        this.slackRoutesProviderFactory = slackRoutesProviderFactory;
    }

    public ImmutableMap.Builder<String, Object> addSlackViewContext(
            final Repository repository,
            final String teamId,
            final ImmutableMap.Builder<String, Object> contextBuilder) {
        List<SlackLink> links = slackLinkManager.getLinks();
        SlackLink link = null;
        if (!StringUtils.isBlank(teamId)) {
            link = slackLinkManager.getLinkByTeamId(teamId).getOrNull();
        }
        // pick a default workspace connection if no specific is provided or isn't valid
        if (link == null && !links.isEmpty()) {
            link = links.get(0);
        }

        if (link != null) {
            contextBuilder.put("link", link);
            UserKey userKey = userManager.getRemoteUserKey();
            if (userKey != null) {
                SlackClient client = slackClientProvider.withLink(link);
                slackUserManager.getByTeamIdAndUserKey(link.getTeamId(), userKey.getStringValue())
                        .flatMap(slackUser -> {
                            contextBuilder.put("slackUserId", slackUser.getSlackUserId());
                            return client.getUserInfo(slackUser.getSlackUserId()).toOptional();
                        })
                        .ifPresent(user -> contextBuilder.put("slackUserName", user.getRealName()));
            }
        }

        contextBuilder.put("routes", slackRoutesProviderFactory.getProvider(repository != null
                ? ImmutableMap.of("repository", repository)
                : Collections.emptyMap()));
        contextBuilder.put("links", links);

        return contextBuilder;
    }

    public ImmutableMap.Builder<String, Object> createGlobalViewContext(final String teamId) {
        NotificationSearchRequest searchRequest = new NotificationSearchRequest.Builder().build();
        return addSlackViewContext(null, teamId, createViewContext(searchRequest));
    }

    public ImmutableMap.Builder<String, Object> createRepositoryViewContext(final String teamId,
                                                                            final Repository repository) {
        NotificationSearchRequest searchRequest = new NotificationSearchRequest.Builder().repository(repository).build();
        final ImmutableMap.Builder<String, Object> contextBuilder = createViewContext(searchRequest);
        contextBuilder.put("repository", repository);
        addSlackViewContext(repository, teamId, contextBuilder);
        return contextBuilder;
    }

    private ImmutableMap.Builder<String, Object> createViewContext(NotificationSearchRequest searchRequest) {
        final ImmutableMap.Builder<String, Object> contextBuilder = new ImmutableMap.Builder<>();

        Page<RepositoryConfiguration> repoConfigs = notificationConfigService.search(searchRequest, PageUtils.newRequest(0, pageSizeLimit));
        contextBuilder.put("configsPage", repoConfigs);

        return contextBuilder;
    }
}
