package com.atlassian.plugins.slack.rest;

import com.atlassian.annotations.security.XsrfProtectionExcluded;
import com.atlassian.confluence.compat.api.service.accessmode.ReadOnlyAccessAllowed;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.atlassian.plugins.slack.analytics.AnalyticsContextProvider;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.events.SlackActionAnalyticEvent;
import com.atlassian.plugins.slack.api.events.SlackBotMentionAnalyticEvent;
import com.atlassian.plugins.slack.api.events.SlackDirectMessageAnalyticEvent;
import com.atlassian.plugins.slack.api.events.SlackLinkSharedAnalyticEvent;
import com.atlassian.plugins.slack.api.events.SlackSlashCommandAnalyticEvent;
import com.atlassian.plugins.slack.api.webhooks.AppHomeOpenedSlackEvent;
import com.atlassian.plugins.slack.api.webhooks.AppUninstalledSlackEvent;
import com.atlassian.plugins.slack.api.webhooks.ChannelArchiveSlackEvent;
import com.atlassian.plugins.slack.api.webhooks.ChannelDeletedSlackEvent;
import com.atlassian.plugins.slack.api.webhooks.ChannelUnarchiveSlackEvent;
import com.atlassian.plugins.slack.api.webhooks.GenericMessageSlackEvent;
import com.atlassian.plugins.slack.api.webhooks.LinkSharedSlackEvent;
import com.atlassian.plugins.slack.api.webhooks.MemberJoinedChannelSlackEvent;
import com.atlassian.plugins.slack.api.webhooks.SlackEvent;
import com.atlassian.plugins.slack.api.webhooks.SlackEventHolder;
import com.atlassian.plugins.slack.api.webhooks.SlackSlashCommand;
import com.atlassian.plugins.slack.api.webhooks.TokensRevokedSlackEvent;
import com.atlassian.plugins.slack.api.webhooks.action.BlockSlackAction;
import com.atlassian.plugins.slack.api.webhooks.action.DialogSubmissionSlackAction;
import com.atlassian.plugins.slack.api.webhooks.action.SlackAction;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.plugins.slack.rest.model.SlackWebHookPayload;
import com.atlassian.plugins.slack.util.AsyncExecutor;
import com.google.common.collect.ImmutableMap;
import com.sun.jersey.spi.container.ResourceFilters;
import lombok.RequiredArgsConstructor;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.apache.commons.lang3.StringUtils.trim;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

/**
 * Entry point for jira and confluence events and slash commands from Slack
 */
@ReadOnlyAccessAllowed
@Path("/")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class SlackWebHookResource {
    public static final String EVENT_TYPE = "type";
    public static final String TYPE_URL_VERIFICATION = "url_verification";

    private static final Logger log = LoggerFactory.getLogger(SlackWebHookResource.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Map<String, Class<? extends SlackEventHolder>> eventByType =
            ImmutableMap.<String, Class<? extends SlackEventHolder>>builder()
                    .put(AppUninstalledSlackEvent.TYPE, AppUninstalledSlackEvent.class)
                    .put(GenericMessageSlackEvent.TYPE, GenericMessageSlackEvent.class)
                    .put(LinkSharedSlackEvent.TYPE, LinkSharedSlackEvent.class)
                    .put(MemberJoinedChannelSlackEvent.TYPE, MemberJoinedChannelSlackEvent.class)
                    .put(TokensRevokedSlackEvent.TYPE, TokensRevokedSlackEvent.class)
                    .put(ChannelArchiveSlackEvent.CHANNEL_TYPE, ChannelArchiveSlackEvent.class)
                    .put(ChannelUnarchiveSlackEvent.CHANNEL_TYPE, ChannelUnarchiveSlackEvent.class)
                    .put(ChannelDeletedSlackEvent.CHANNEL_TYPE, ChannelDeletedSlackEvent.class)
                    .put(ChannelArchiveSlackEvent.GROUP_TYPE, ChannelArchiveSlackEvent.class)
                    .put(ChannelUnarchiveSlackEvent.GROUP_TYPE, ChannelUnarchiveSlackEvent.class)
                    .put(ChannelDeletedSlackEvent.GROUP_TYPE, ChannelDeletedSlackEvent.class)
                    .put(AppHomeOpenedSlackEvent.TYPE, AppHomeOpenedSlackEvent.class)
                    .build();
    private static final Map<String, Class<? extends SlackAction>> actionByType =
            ImmutableMap.<String, Class<? extends SlackAction>>builder()
                    .put(DialogSubmissionSlackAction.TYPE, DialogSubmissionSlackAction.class)
                    .put(BlockSlackAction.TYPE, BlockSlackAction.class)
                    .build();

    private final SlackLinkManager slackLinkManager;
    private final EventPublisher eventPublisher;
    private final AsyncExecutor asyncExecutor;
    private final AnalyticsContextProvider analyticsContextProvider;

    @POST
    @Path("/event")
    @Consumes(MediaType.APPLICATION_JSON)
    @AnonymousAllowed
    @ResourceFilters(SlackSignatureVerifyingFilter.class)
    public Response webEvent(@Context final HttpServletRequest request, final JsonNode eventPayload) {
        if (TYPE_URL_VERIFICATION.equals(eventPayload.path(EVENT_TYPE).getTextValue())) {
            return Response
                    .ok(eventPayload.path("challenge").getTextValue())
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }

        if (log.isDebugEnabled()) {
            try {
                log.debug("Event received: {}", OBJECT_MAPPER.writeValueAsString(eventPayload));
            } catch (IOException e) {
                //nothing
            }
        }

        try {
            final SlackWebHookPayload event = OBJECT_MAPPER.treeToValue(eventPayload, SlackWebHookPayload.class);
            final Optional<SlackLink> link = slackLinkManager.getLinkByTeamId(event.getTeamId()).toOptional();
            if (link.isPresent()) {
                final SlackEvent genericSlackEvent = new SlackEvent(
                        event.getTeamId(),
                        event.getEventId(),
                        event.getEventTime(),
                        link.get());

                String eventTypeName = event.getType();
                final Class<? extends SlackEventHolder> eventType = eventByType.get(eventTypeName);
                if (eventType != null) {
                    final SlackEventHolder specificEvent = OBJECT_MAPPER.treeToValue(event.getEvent(), eventType);
                    specificEvent.setSlackEvent(genericSlackEvent);

                    // send analytic event first in case of specific event received: link shared, direct or bot mention
                    if (specificEvent instanceof LinkSharedSlackEvent) {
                        eventPublisher.publish(new SlackLinkSharedAnalyticEvent(analyticsContextProvider.byTeamIdAndSlackUserId(
                                genericSlackEvent.getTeamId(), ((LinkSharedSlackEvent) specificEvent).getUser())));
                    } else if (specificEvent instanceof GenericMessageSlackEvent) {
                        GenericMessageSlackEvent messageEvent = (GenericMessageSlackEvent) specificEvent;
                        String slackUserId = ((GenericMessageSlackEvent) specificEvent).getUser();
                        if ("im".equals(messageEvent.getChannelType()) && !"bot_message".equals(messageEvent.getSubtype())) {
                            eventPublisher.publish(new SlackDirectMessageAnalyticEvent(
                                    analyticsContextProvider.byTeamIdAndSlackUserId(genericSlackEvent.getTeamId(), slackUserId)));
                        } else {
                            if (trimToEmpty(messageEvent.getText()).contains("@" + genericSlackEvent.getSlackLink().getBotUserId())) {
                                eventPublisher.publish(new SlackBotMentionAnalyticEvent(
                                        analyticsContextProvider.byTeamIdAndSlackUserId(genericSlackEvent.getTeamId(), slackUserId)));
                            }
                        }
                    }

                    eventPublisher.publish(specificEvent);
                } else {
                    eventPublisher.publish(genericSlackEvent);
                }
            } else {
                log.warn("Unauthorized or invalid Slack WebHook: {}", event);
            }
        } catch (Exception e) {
            log.warn("Error parsing event payload", e);
        }
        return Response.ok().build();
    }

    @POST
    @Path("/command")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    @AnonymousAllowed
    @XsrfProtectionExcluded
    @ResourceFilters(SlackSignatureVerifyingFilter.class)
    public Response slashCommand(
            @HeaderParam("X-Slack-Signature") final String signingSecret,
            @FormParam("token") String verificationToken,
            @FormParam("team_id") String teamId,
            @FormParam("team_domain") String teamDomain,
            @FormParam("enterprise_id") String enterpriseId,
            @FormParam("enterprise_name") String enterpriseName,
            @FormParam("channel_id") String channelId,
            @FormParam("channel_name") String channelName,
            @FormParam("user_id") String userId,
            @FormParam("user_name") String userName,
            @FormParam("command") String commandName,
            @FormParam("text") String text,
            @FormParam("response_url") String responseUrl,
            @FormParam("trigger_id") String triggerId
    ) {
        slackLinkManager.getLinkByTeamId(teamId)
                .map(link -> new SlackSlashCommand(
                        verificationToken,
                        teamId,
                        teamDomain,
                        enterpriseId,
                        enterpriseName,
                        channelId,
                        channelName,
                        userId,
                        userName,
                        commandName,
                        text,
                        responseUrl,
                        triggerId,
                        link))
                .fold(
                        e -> {
                            log.warn("Unauthorized or invalid Slack Slash Command teamId={} channelId={}",
                                    teamId, channelId, e);
                            return null;
                        },
                        command -> {
                            String subCommand = trim(substringBefore(text, " "));
                            eventPublisher.publish(new SlackSlashCommandAnalyticEvent(analyticsContextProvider
                                    .byTeamIdAndSlackUserId(teamId, userId), subCommand));
                            eventPublisher.publish(command);
                            return null;
                        }
                );
        return Response.ok().build();
    }

    @POST
    @Path("/action")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    @AnonymousAllowed
    @XsrfProtectionExcluded
    @ResourceFilters(SlackSignatureVerifyingFilter.class)
    public Response action(@FormParam("payload") String payload) {
        asyncExecutor.run(() -> {
            log.debug("Received action {}", payload);
            try {
                JsonNode payloadNode = OBJECT_MAPPER.readTree(payload);
                String type = payloadNode.get("type").asText();

                Class<? extends SlackAction> actionType = actionByType.get(type);
                if (actionType != null) {
                    SlackAction action = OBJECT_MAPPER.readValue(payload, actionType);
                    eventPublisher.publish(new SlackActionAnalyticEvent(analyticsContextProvider
                            .byTeamIdAndSlackUserId(action.getTeamId(), action.getUserId()), action.getType()));
                    eventPublisher.publish(action);
                }

            } catch (IOException e) {
                log.error("Error parsing action payload", e);
            }
        });

        return Response.ok().build();
    }
}

