package com.atlassian.confluence.plugins.slack.spacetochannel.dataprovider;

import com.atlassian.confluence.plugins.slack.spacetochannel.notifications.SpaceNotificationContext;
import com.atlassian.confluence.plugins.slack.spacetochannel.service.SlackSpaceToChannelService;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.ContextProvider;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.atlassian.plugins.slack.api.descriptor.NotificationTypeService;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.plugins.slack.rest.model.LimitedSlackLinkDto;
import com.atlassian.plugins.slack.spi.SlackRoutesProviderFactory;
import com.atlassian.plugins.slack.user.SlackUserManager;
import com.atlassian.sal.api.user.UserManager;
import com.github.seratch.jslack.api.model.User;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

public class SlackGlobalAdminDataProvider implements ContextProvider {
    private final SlackSpaceToChannelService spaceToChannelService;
    private final UserManager userManager;
    private final SlackUserManager slackUserManager;
    private final SlackLinkManager slackLinkManager;
    private final NotificationTypeService notificationTypeService;
    private final SlackClientProvider slackClientProvider;
    private final SlackRoutesProviderFactory slackRoutesProviderFactory;

    SlackGlobalAdminDataProvider(final SlackSpaceToChannelService spaceToChannelService,
                                 @Qualifier("salUserManager") final UserManager userManager,
                                 final SlackUserManager slackUserManager,
                                 final SlackLinkManager slackLinkManager,
                                 final NotificationTypeService notificationTypeService,
                                 final SlackClientProvider slackClientProvider,
                                 final SlackRoutesProviderFactory slackRoutesProviderFactory) {
        this.spaceToChannelService = spaceToChannelService;
        this.userManager = userManager;
        this.slackUserManager = slackUserManager;
        this.slackLinkManager = slackLinkManager;
        this.notificationTypeService = notificationTypeService;
        this.slackClientProvider = slackClientProvider;
        this.slackRoutesProviderFactory = slackRoutesProviderFactory;
    }

    @Override
    public void init(final Map<String, String> stringStringMap) throws PluginParseException {
        // nothing
    }

    @Override
    public Map<String, Object> getContextMap(final Map<String, Object> context) {
        final Collection<LimitedSlackLinkDto> links = slackLinkManager.getLinks()
                .stream()
                .map(LimitedSlackLinkDto::new)
                .collect(Collectors.toList());
        context.put("links", links);
        context.put("configs", spaceToChannelService.getAllSpaceToChannelConfigurations());
        context.put("notificationTypes", notificationTypeService.getNotificationTypes(SpaceNotificationContext.KEY));
        context.put("routes", slackRoutesProviderFactory.getProvider(Collections.emptyMap()));

        final SlackLink link = (SlackLink) context.get("link");
        if (link != null && userManager.getRemoteUserKey() != null) {
            slackUserManager.getByTeamIdAndUserKey(link.getTeamId(), userManager.getRemoteUserKey().getStringValue())
                    .map(slackUser -> {
                        context.put("slackUserId", slackUser.getSlackUserId());
                        return slackClientProvider.withLink(link).getUserInfo(slackUser.getSlackUserId()).fold(
                                e -> slackUser.getSlackUserId(),
                                User::getRealName
                        );
                    })
                    .ifPresent(userName -> context.put("slackUserName", userName));
        }


        return context;
    }

}
