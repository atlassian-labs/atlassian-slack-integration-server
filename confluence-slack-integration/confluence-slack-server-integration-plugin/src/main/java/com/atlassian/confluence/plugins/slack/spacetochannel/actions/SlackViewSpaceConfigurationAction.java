package com.atlassian.confluence.plugins.slack.spacetochannel.actions;

import com.atlassian.confluence.plugins.slack.spacetochannel.configuration.SpaceToChannelConfiguration;
import com.atlassian.confluence.plugins.slack.spacetochannel.events.analytic.ConfluencePage;
import com.atlassian.confluence.plugins.slack.spacetochannel.notifications.SpaceNotificationContext;
import com.atlassian.confluence.plugins.slack.spacetochannel.service.SlackSpaceToChannelService;
import com.atlassian.confluence.spaces.actions.AbstractSpaceAdminAction;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugins.slack.analytics.AnalyticsContextProvider;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.atlassian.plugins.slack.api.descriptor.NotificationTypeService;
import com.atlassian.plugins.slack.api.events.PageVisitedEvent;
import com.atlassian.plugins.slack.api.notification.NotificationType;
import com.atlassian.plugins.slack.api.routes.SlackRoutesProvider;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.plugins.slack.spi.SlackRoutesProviderFactory;
import com.atlassian.plugins.slack.user.SlackUserManager;
import com.atlassian.struts.httpmethod.PermittedMethods;
import com.github.seratch.jslack.api.model.User;
import com.google.common.collect.ImmutableMap;
import lombok.RequiredArgsConstructor;
import org.apache.struts2.action.Action;

import java.util.List;

import static com.atlassian.struts.httpmethod.HttpMethod.GET;
import static com.google.common.collect.Lists.newArrayList;

@RequiredArgsConstructor
public class SlackViewSpaceConfigurationAction extends AbstractSpaceAdminAction {
    private final SlackSpaceToChannelService slackSpaceToChannelService;
    private final SlackLinkManager slackLinkManager;
    private final SlackUserManager slackUserManager;
    private final SlackRoutesProviderFactory slackRoutesProviderFactory;
    private final NotificationTypeService notificationTypeService;
    private final SlackClientProvider slackClientProvider;
    private final EventPublisher eventPublisher;
    private final AnalyticsContextProvider analyticsContextProvider;

    private List<SpaceToChannelConfiguration> configs;
    private List<SlackLink> links;
    private SlackLink link;
    private SlackRoutesProvider routesProvider;
    private String teamId;

    @PermittedMethods(GET)
    @Override
    public String execute() {
        List<SlackLink> links = slackLinkManager.getLinks();
        if (links.isEmpty()) {
            return "install";
        }
        this.links = links;

        if (teamId == null) {
            teamId = links.get(0).getTeamId();
        }
        this.link = links.stream()
                .filter(l -> l.getTeamId().equals(teamId))
                .findFirst()
                .orElse(null);

        this.routesProvider = slackRoutesProviderFactory.getProvider(ImmutableMap.of("space", getSpace()));
        this.configs = newArrayList(slackSpaceToChannelService.getSpaceToChannelConfiguration(getSpaceKey()));

        eventPublisher.publish(new PageVisitedEvent(analyticsContextProvider.bySlackLink(link), ConfluencePage.SPACE_CONFIG));

        return Action.SUCCESS;
    }

    @SuppressWarnings("unused")
    public void setTeamId(final String teamId) {
        this.teamId = teamId;
    }

    public SlackLink getLink() {
        return link;
    }

    public List<SlackLink> getLinks() {
        return links;
    }

    public String getSlackUserName() {
        return slackUserManager.getByTeamIdAndUserKey(teamId, getAuthenticatedUser().getKey().getStringValue())
                .map(slackUser -> slackClientProvider.withLink(link).getUserInfo(slackUser.getSlackUserId())
                        .fold(e -> slackUser.getSlackUserId(), User::getRealName))
                .orElse(null);
    }

    public SlackRoutesProvider getRoutes() {
        return routesProvider;
    }

    public List<SpaceToChannelConfiguration> getConfigs() {
        return configs;
    }

    public List<NotificationType> getNotificationTypes() {
        return notificationTypeService.getNotificationTypes(SpaceNotificationContext.KEY);
    }
}
