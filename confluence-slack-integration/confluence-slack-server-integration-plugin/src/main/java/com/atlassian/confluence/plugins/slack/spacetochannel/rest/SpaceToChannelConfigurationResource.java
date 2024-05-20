package com.atlassian.confluence.plugins.slack.spacetochannel.rest;

import com.atlassian.confluence.compat.api.service.accessmode.ReadOnlyAccessAllowed;
import com.atlassian.confluence.plugins.slack.spacetochannel.configuration.SlackChannelDefinition;
import com.atlassian.confluence.plugins.slack.spacetochannel.configuration.SpaceToChannelSettings;
import com.atlassian.confluence.plugins.slack.spacetochannel.events.SpaceToChannelLinkedEvent;
import com.atlassian.confluence.plugins.slack.spacetochannel.events.analytic.SpaceToChannelLinkedAnalyticEvent;
import com.atlassian.confluence.plugins.slack.spacetochannel.events.analytic.SpaceToChannelNotificationDisabledAnalyticEvent;
import com.atlassian.confluence.plugins.slack.spacetochannel.events.analytic.SpaceToChannelNotificationEnabledAnalyticEvent;
import com.atlassian.confluence.plugins.slack.spacetochannel.events.analytic.SpaceToChannelUnlinkedAnalyticEvent;
import com.atlassian.confluence.plugins.slack.spacetochannel.notifications.SpaceNotificationContext;
import com.atlassian.confluence.plugins.slack.spacetochannel.service.SlackSpaceToChannelService;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.spaces.SpaceManager;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugins.slack.analytics.AnalyticsContext;
import com.atlassian.plugins.slack.analytics.AnalyticsContextProvider;
import com.atlassian.plugins.slack.api.ConversationKey;
import com.atlassian.plugins.slack.api.descriptor.NotificationTypeService;
import com.atlassian.plugins.slack.api.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Optional;
import java.util.Set;

import static com.atlassian.confluence.security.Permission.ADMINISTER;
import static com.atlassian.confluence.security.PermissionManager.TARGET_APPLICATION;

/**
 * Resources for updating space to channel mappings for a space.
 * <p>
 * Requires space admin.
 */
@ReadOnlyAccessAllowed
@Path("/config/{spaceKey}")
@Slf4j
@RequiredArgsConstructor(onConstructor_ = {@Autowired, @Inject})
public class SpaceToChannelConfigurationResource {
    private final SpaceManager spaceManager;
    private final PermissionManager permissionManager;
    private final SlackSpaceToChannelService slackSpaceToChannelService;
    private final EventPublisher eventPublisher;
    private final NotificationTypeService notificationTypeService;
    private final AnalyticsContextProvider analyticsContextProvider;

    @PUT
    @Path("{teamId}/{channelId}/{notificationName}")
    public Response enableNotification(@PathParam("spaceKey") String spaceKey,
                                       @PathParam("teamId") String teamId,
                                       @PathParam("channelId") String channelId,
                                       @PathParam("notificationName") String notificationName,
                                       @QueryParam("initialLink") @DefaultValue("false") boolean initialLink) {

        final Space space = spaceManager.getSpace(spaceKey);
        if (!isSpaceAdmin(space)) {
            return Response.status(Status.FORBIDDEN).build();
        }

        final Optional<NotificationType> spaceToChannelNotificationOption =
                notificationTypeService.getNotificationTypeForKey(notificationName);
        if (spaceToChannelNotificationOption.isPresent()) {
            NotificationType notificationType = spaceToChannelNotificationOption.get();

            if (slackSpaceToChannelService.hasMappingForEntityChannelAndType(spaceKey, channelId, notificationType)) {
                return Response.ok().build();
            }

            SlackChannelDefinition channel = new SlackChannelDefinition(teamId, teamId, channelId, channelId, true);

            ConfluenceUser user = AuthenticatedUserThreadLocal.get();
            String userKey = user.getKey().getStringValue();
            slackSpaceToChannelService.addNotificationForSpaceAndChannel(
                    spaceKey,
                    userKey,
                    teamId,
                    channelId,
                    notificationType);

            AnalyticsContext context = analyticsContextProvider.byTeamIdAndUserKey(teamId, userKey);
            long spaceId = space.getId();
            if (initialLink) {
                eventPublisher.publish(new SpaceToChannelLinkedEvent(space, channel, user));
                eventPublisher.publish(new SpaceToChannelLinkedAnalyticEvent(context, spaceId, channelId));
            }

            eventPublisher.publish(new SpaceToChannelNotificationEnabledAnalyticEvent(context, spaceId, channelId, notificationType.getKey()));

            return Response.ok().build();
        } else {
            String message = "Unable to convert '" + notificationName + "' to an enum of type SpaceToChannelNotification";
            log.debug(message);
            return Response.status(Status.BAD_REQUEST).entity(message).build();
        }
    }

    @DELETE
    @Path("{teamId}/{channelId}/{notificationName}")
    public Response removeNotification(@PathParam("spaceKey") String spaceKey,
                                       @PathParam("teamId") String teamId,
                                       @PathParam("channelId") String channelId,
                                       @PathParam("notificationName") String notificationName) {

        if (!isSpaceAdmin(spaceKey)) {
            return Response.status(Status.FORBIDDEN).build();
        }

        final Optional<NotificationType> spaceToChannelNotificationOption =
                notificationTypeService.getNotificationTypeForKey(notificationName);
        if (spaceToChannelNotificationOption.isPresent()) {
            NotificationType notificationType = spaceToChannelNotificationOption.get();
            Optional<SpaceToChannelSettings> settingsOption = slackSpaceToChannelService.getSpaceToChannelSettings(spaceKey, channelId);

            if (!settingsOption.isPresent()) {
                // This is ok, if a another user has done it and the current user has a stale config page.
                String message = "Unable to find settings for space/channel '" + spaceKey + "/" + channelId;
                log.debug(message);
                return Response.ok().build();
            }

            SpaceToChannelSettings settings = settingsOption.get();

            //TODO: should we leave channelName null instead of setting it to channelId?
            SlackChannelDefinition channel = new SlackChannelDefinition(teamId, teamId, channelId, channelId, true);

            final Space space = spaceManager.getSpace(spaceKey);

            if (!settings.getNotificationTypes().contains(notificationType)) {
                // This is ok, if a another user has done it and the current user has a stale config page.
                String message = "Notification type (" + notificationType.getKey() + ") is not set for space/channel '" + spaceKey + "/" + channelId;
                log.debug(message);
                return Response.ok().build();
            }

            slackSpaceToChannelService.removeNotificationForSpaceAndChannel(spaceKey, new ConversationKey(teamId, channelId), notificationType);

            String userKey = AuthenticatedUserThreadLocal.get().getKey().getStringValue();
            eventPublisher.publish(new SpaceToChannelNotificationDisabledAnalyticEvent(analyticsContextProvider.byTeamIdAndUserKey(
                    teamId, userKey), space.getId(), channelId, notificationType.getKey()));

            return Response.ok().build();
        } else {
            String message = "Unable to convert '" + notificationName + "' to an enum of type SpaceToChannelNotification";
            log.debug(message);
            return Response.status(Status.BAD_REQUEST).entity(message).build();
        }
    }

    @DELETE
    @Path("{teamId}/{channelId}")
    public Response removeNotificationsForChannel(
            @PathParam("spaceKey") String spaceKey,
            @PathParam("teamId") String teamId,
            @PathParam("channelId") String channelId) {

        if (!isSpaceAdmin(spaceKey)) {
            return Response.status(Status.FORBIDDEN).build();
        }

        Optional<SpaceToChannelSettings> settingsOption = slackSpaceToChannelService.getSpaceToChannelSettings(spaceKey, channelId);
        if (!settingsOption.isPresent()) {
            // This is ok, if a another user has done it and the current user has a stale config page.
            return Response.ok().build();
        }

        SpaceToChannelSettings settings = settingsOption.get();
        Set<NotificationType> configuredNotificationTypes = settings.getNotificationTypes();

        final Space space = spaceManager.getSpace(spaceKey);
        ConfluenceUser user = AuthenticatedUserThreadLocal.get();
        String userKey = user.getKey().getStringValue();
        for (NotificationType notificationType : notificationTypeService.getNotificationTypes(SpaceNotificationContext.KEY)) {
            if (configuredNotificationTypes.contains(notificationType)) {
                eventPublisher.publish(new SpaceToChannelNotificationDisabledAnalyticEvent(
                        analyticsContextProvider.byTeamIdAndUserKey(teamId, userKey), space.getId(), channelId,
                        notificationType.getKey()));
            }
        }

        slackSpaceToChannelService.removeNotificationsForSpaceAndChannel(spaceKey, new ConversationKey(teamId, channelId));

        eventPublisher.publish(new SpaceToChannelUnlinkedAnalyticEvent(analyticsContextProvider.byTeamIdAndUserKey(
                teamId, userKey), space.getId(), channelId));

        return Response.ok().build();
    }

    private boolean isSpaceAdmin(String spaceKey) {
        return isSpaceAdmin(spaceManager.getSpace(spaceKey));
    }

    private boolean isSpaceAdmin(Space space) {
        final ConfluenceUser user = AuthenticatedUserThreadLocal.get();
        return permissionManager.hasPermission(user, ADMINISTER, space) ||
                permissionManager.hasPermission(user, ADMINISTER, TARGET_APPLICATION);
    }
}
