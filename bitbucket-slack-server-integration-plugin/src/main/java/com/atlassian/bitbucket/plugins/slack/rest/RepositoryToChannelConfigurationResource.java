package com.atlassian.bitbucket.plugins.slack.rest;

import com.atlassian.bitbucket.AuthorisationException;
import com.atlassian.bitbucket.permission.Permission;
import com.atlassian.bitbucket.permission.PermissionValidationService;
import com.atlassian.bitbucket.plugins.slack.event.RepositoryLinkedEvent;
import com.atlassian.bitbucket.plugins.slack.event.analytic.RepositoryLinkedAnalyticEvent;
import com.atlassian.bitbucket.plugins.slack.event.analytic.RepositoryUnlinkedAnalyticEvent;
import com.atlassian.bitbucket.plugins.slack.notification.NotificationUtil;
import com.atlassian.bitbucket.plugins.slack.notification.configuration.NotificationConfigurationService;
import com.atlassian.bitbucket.plugins.slack.notification.configuration.NotificationDisableRequest;
import com.atlassian.bitbucket.plugins.slack.notification.configuration.NotificationEnableRequest;
import com.atlassian.bitbucket.plugins.slack.settings.BitbucketSlackSettingsService;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.repository.RepositoryService;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugins.slack.analytics.AnalyticsContextProvider;
import com.atlassian.plugins.slack.api.notification.Verbosity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

/**
 * Resources for updating repo to channel mappings for a repository.
 * <p>
 * Requires repository admin.
 */
@Path("/config/{repositoryId}")
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class RepositoryToChannelConfigurationResource {
    private final RepositoryService repositoryService;
    private final PermissionValidationService permissionValidationService;
    private final EventPublisher eventPublisher;
    private final NotificationConfigurationService notificationConfigurationService;
    private final BitbucketSlackSettingsService bitbucketSlackSettingsService;
    private final AnalyticsContextProvider analyticsContextProvider;

    @PUT
    @Path("{teamId}/{channelId}/{notificationKey}")
    public Response enableNotification(@PathParam("repositoryId") int repositoryId,
                                       @PathParam("teamId") String teamId,
                                       @PathParam("channelId") String channelId,
                                       @PathParam("notificationKey") String notificationKey,
                                       @QueryParam("initialLink") @DefaultValue("false") boolean initialLink) {
        Repository repository = repositoryService.getById(repositoryId);
        if (repository == null || !isRepositoryAdmin(repository)) {
            return Response.status(Status.FORBIDDEN).build();
        }

        if (initialLink) {
            notificationConfigurationService.enable(new NotificationEnableRequest.Builder()
                    .teamId(teamId)
                    .channelId(channelId)
                    .repository(repository)
                    .notificationTypes(NotificationUtil.ALL_NOTIFICATION_TYPE_KEYS)
                    .build());
            bitbucketSlackSettingsService.setVerbosity(repositoryId, teamId, channelId, Verbosity.EXTENDED);

            eventPublisher.publish(new RepositoryLinkedEvent(this, repository, teamId, channelId));
            eventPublisher.publish(new RepositoryLinkedAnalyticEvent(analyticsContextProvider.byTeamId(teamId),
                    repositoryId, channelId));

            return Response.ok().build();
        } else if (NotificationUtil.ALL_NOTIFICATION_TYPE_KEYS.contains(notificationKey)) {
            notificationConfigurationService.enable(new NotificationEnableRequest.Builder()
                    .teamId(teamId)
                    .channelId(channelId)
                    .repository(repository)
                    .notificationType(notificationKey)
                    .build());

            return Response.ok().build();
        } else {
            final String message = "Notification '" + notificationKey + "' is not registered";
            log.debug(message);
            return Response.status(Status.BAD_REQUEST).entity(message).build();
        }
    }

    @DELETE
    @Path("{teamId}/{channelId}/{notificationKey}")
    public Response removeNotification(@PathParam("repositoryId") int repositoryId,
                                       @PathParam("teamId") String teamId,
                                       @PathParam("channelId") String channelId,
                                       @PathParam("notificationKey") String notificationKey) {
        Repository repository = repositoryService.getById(repositoryId);
        if (repository == null || !isRepositoryAdmin(repository)) {
            return Response.status(Status.FORBIDDEN).build();
        }

        if (NotificationUtil.ALL_NOTIFICATION_TYPE_KEYS.contains(notificationKey)) {
            notificationConfigurationService.disable(new NotificationDisableRequest.Builder()
                    .teamId(teamId)
                    .channelId(channelId)
                    .repository(repository)
                    .notificationType(notificationKey)
                    .build());

            return Response.ok().build();
        } else {
            String message = "Notification '" + notificationKey + "' is not registered";
            log.debug(message);
            return Response.status(Status.BAD_REQUEST).entity(message).build();
        }
    }

    @DELETE
    @Path("{teamId}/{channelId}")
    public Response removeNotificationsForChannel(@PathParam("repositoryId") int repositoryId,
                                                  @PathParam("teamId") String teamId,
                                                  @PathParam("channelId") String channelId) {
        Repository repository = repositoryService.getById(repositoryId);
        if (repository == null || !isRepositoryAdmin(repository)) {
            return Response.status(Status.FORBIDDEN).build();
        }

        // do not pass any specific notification to remove;
        // all of them (for specified repo and channel) will be removed in this case
        NotificationDisableRequest request = new NotificationDisableRequest.Builder()
                .teamId(teamId)
                .channelId(channelId)
                .repository(repository)
                .build();
        notificationConfigurationService.disable(request);
        bitbucketSlackSettingsService.clearVerbosity(repositoryId, teamId, channelId);

        eventPublisher.publish(new RepositoryUnlinkedAnalyticEvent(analyticsContextProvider.byTeamId(teamId),
                repositoryId, channelId));

        return Response.ok().build();
    }

    @PUT
    @Path("{teamId}/{channelId}/option/{optionName}/{optionValue}")
    public Response saveOption(@PathParam("repositoryId") int repositoryId,
                               @PathParam("teamId") String teamId,
                               @PathParam("channelId") String channelId,
                               @PathParam("optionName") String optionName,
                               @PathParam("optionValue") String optionValue) {
        Repository repository = repositoryService.getById(repositoryId);
        if (repository == null || !isRepositoryAdmin(repository)) {
            return Response.status(Status.FORBIDDEN).build();
        }

        switch (optionName) {
            case "verbosity":
                Verbosity verbosity = Verbosity.valueOf(optionValue);
                bitbucketSlackSettingsService.setVerbosity(repositoryId, teamId, channelId, verbosity);
                break;
        }

        return Response.ok().build();
    }

    private boolean isRepositoryAdmin(Repository repository) {
        boolean isAdmin = true;
        try {
            permissionValidationService.validateForRepository(repository, Permission.REPO_ADMIN);
        } catch (AuthorisationException e) {
            isAdmin = false;
        }

        return isAdmin;
    }
}
