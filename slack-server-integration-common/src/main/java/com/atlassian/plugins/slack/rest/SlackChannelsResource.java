package com.atlassian.plugins.slack.rest;

import com.atlassian.confluence.compat.api.service.accessmode.ReadOnlyAccessAllowed;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugins.slack.analytics.AnalyticsContextProvider;
import com.atlassian.plugins.slack.api.client.SlackClient;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.atlassian.plugins.slack.event.ChannelCreatedEvent;
import com.atlassian.plugins.slack.rest.model.SlackChannelDTO;
import com.atlassian.plugins.slack.util.ErrorResponse;
import com.google.common.collect.ImmutableMap;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.stream.Collectors;

import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;

@ReadOnlyAccessAllowed
@Path("channels")
@RequiredArgsConstructor(onConstructor_ = {@Autowired, @Inject})
public class SlackChannelsResource {
    private final SlackClientProvider slackClientProvider;
    private final EventPublisher eventPublisher;
    private final AnalyticsContextProvider analyticsContextProvider;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllChannels(@QueryParam("teamId") final String teamId) {
        return slackClientProvider
                .withTeamId(teamId)
                .flatMap(SlackClient::withRemoteUser)
                .flatMap(client -> client.getAllConversations()
                        .map(conversations -> conversations.stream()
                                .map(conversation -> new SlackChannelDTO(
                                        teamId,
                                        client.getLink().getTeamName(),
                                        conversation.getId(),
                                        conversation.getName(),
                                        conversation.isPrivate()))
                                .collect(Collectors.toList())
                        )
                        .leftMap(ErrorResponse::getException))
                .fold(
                        e -> Response.serverError().entity(e.getMessage()).type(MediaType.TEXT_PLAIN_TYPE).build(),
                        list -> Response.ok(list).build()
                );
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createChannel(@QueryParam("channelName") final String channelName,
                                  @QueryParam("teamId") final String teamId) {
        if (StringUtils.isBlank(channelName)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("No channel name provided.")
                    .build();
        }

        return slackClientProvider
                .withTeamId(teamId)
                .flatMap(SlackClient::withRemoteUser)
                .flatMap(client -> client.createConversation(channelName)
                        .map(conversation -> new SlackChannelDTO(
                                teamId,
                                client.getLink().getTeamName(),
                                conversation.getId(),
                                conversation.getName(),
                                conversation.isPrivate())
                        )
                        .leftMap(ErrorResponse::getException))
                .fold(
                        e -> Response
                                .status(Response.Status.BAD_REQUEST)
                                .entity(e.getMessage()).type(MediaType.TEXT_PLAIN_TYPE)
                                .build(),
                        dto -> {
                            publishChannelCreatedEvent(dto);
                            return Response.ok(dto).build();
                        }
                );
    }

    @GET
    @Path("{teamId}/{channelId}/{messageId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getMessagePermalink(@PathParam("teamId") final String teamId,
                                        @PathParam("channelId") final String channelId,
                                        @PathParam("messageId") final String messageId) {
        return slackClientProvider.withTeamId(teamId)
                .leftMap(ErrorResponse::new)
                .flatMap(client -> client.getPermalink(channelId, messageId))
                .fold(
                        e -> Response.status(NOT_FOUND).build(),
                        link -> Response.ok(ImmutableMap.<String, Object>of("link", link)).build()
                );
    }

    private void publishChannelCreatedEvent(final SlackChannelDTO createdChannel) {
        eventPublisher.publish(new ChannelCreatedEvent(
                analyticsContextProvider.byTeamId(createdChannel.getTeamId()),
                createdChannel.getChannelId(),
                createdChannel.isPrivate()));
    }
}
