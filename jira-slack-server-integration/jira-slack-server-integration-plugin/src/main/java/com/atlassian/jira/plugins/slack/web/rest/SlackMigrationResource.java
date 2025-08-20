package com.atlassian.jira.plugins.slack.web.rest;

import com.atlassian.plugins.slack.api.client.SlackClient;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.plugins.slack.rest.model.SlackChannelDTO;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Path("/migration")
public class SlackMigrationResource {
    private final SlackLinkManager slackLinkManager;
    private final SlackClientProvider slackClientProvider;

    @Inject
    public SlackMigrationResource(final SlackLinkManager slackLinkManager,
                                  final SlackClientProvider slackClientProvider) {
        this.slackLinkManager = slackLinkManager;
        this.slackClientProvider = slackClientProvider;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMigrationChannels() {
        return Response.ok(
                slackLinkManager.getLinks().parallelStream()
                        .map(slackClientProvider::withLink)
                        .map(SlackClient::withRemoteUserTokenIfAvailable)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .flatMap(client -> client.getAllConversations().fold(
                                e -> Stream.empty(),
                                conversations -> conversations.stream()
                                        .map(conversation -> new SlackChannelDTO(
                                                client.getLink().getTeamId(),
                                                client.getLink().getTeamName(),
                                                conversation.getId(),
                                                conversation.getName(),
                                                conversation.isPrivate()
                                        ))
                        ))
                        .collect(Collectors.toList()))
                .build();
    }
}
