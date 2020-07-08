package com.atlassian.bitbucket.plugins.slack.listener;

import com.atlassian.bitbucket.plugins.slack.event.analytic.BitbucketNotificationSentEvent;
import com.atlassian.bitbucket.plugins.slack.event.analytic.BitbucketNotificationSentEvent.Type;
import com.atlassian.bitbucket.plugins.slack.model.Unfurl;
import com.atlassian.bitbucket.plugins.slack.notification.NotificationPublisher;
import com.atlassian.bitbucket.plugins.slack.notification.renderer.SlackNotificationRenderer;
import com.atlassian.bitbucket.user.ApplicationUser;
import com.atlassian.bitbucket.user.UserService;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugins.slack.analytics.AnalyticsContext;
import com.atlassian.plugins.slack.analytics.AnalyticsContextProvider;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.SlackUser;
import com.atlassian.plugins.slack.api.client.SlackClient;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.atlassian.plugins.slack.api.webhooks.GenericMessageSlackEvent;
import com.atlassian.plugins.slack.api.webhooks.LinkSharedSlackEvent;
import com.atlassian.plugins.slack.api.webhooks.SlackSlashCommand;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.plugins.slack.user.SlackUserManager;
import com.atlassian.plugins.slack.util.AsyncExecutor;
import com.atlassian.plugins.slack.util.LinkHelper;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.UrlMode;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostEphemeralRequest;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest.ChatPostMessageRequestBuilder;
import com.github.seratch.jslack.api.model.Attachment;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.atlassian.plugins.slack.util.SlackHelper.removeSlackLinks;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Component
@Slf4j
public class SlackMessageEventListener {
    private final SlackUserManager slackUserManager;
    private final SlackLinkManager slackLinkManager;
    private final SlackClientProvider slackClientProvider;
    private final AsyncExecutor asyncExecutor;
    private final SlackNotificationRenderer slackNotificationRenderer;
    private final UserService userService;
    private final ApplicationProperties applicationProperties;
    private final UnfurlLinkExtractor unfurlLinkExtractor;
    private final NotificationPublisher notificationPublisher;
    private final EventPublisher eventPublisher;
    private final AnalyticsContextProvider analyticsContextProvider;

    @Autowired
    public SlackMessageEventListener(
            final SlackUserManager slackUserManager,
            final SlackLinkManager slackLinkManager,
            final SlackClientProvider slackClientProvider,
            final AsyncExecutor asyncExecutor,
            final SlackNotificationRenderer slackNotificationRenderer,
            final UserService userService,
            @Qualifier("salApplicationProperties") final ApplicationProperties applicationProperties,
            final UnfurlLinkExtractor unfurlLinkExtractor,
            final NotificationPublisher notificationPublisher,
            final EventPublisher eventPublisher,
            final AnalyticsContextProvider analyticsContextProvider) {
        this.slackUserManager = slackUserManager;
        this.slackLinkManager = slackLinkManager;
        this.slackClientProvider = slackClientProvider;
        this.asyncExecutor = asyncExecutor;
        this.slackNotificationRenderer = slackNotificationRenderer;
        this.userService = userService;
        this.applicationProperties = applicationProperties;
        this.unfurlLinkExtractor = unfurlLinkExtractor;
        this.notificationPublisher = notificationPublisher;
        this.eventPublisher = eventPublisher;
        this.analyticsContextProvider = analyticsContextProvider;
    }

    @EventListener
    public void slashCommand(@Nonnull final SlackSlashCommand command) {
        log.debug("Got slash command {}", command.getCommandName());

        final String commandText = StringUtils.trimToEmpty(command.getText());
        ChatPostMessageRequestBuilder responseMessage = handleSlashCommand(commandText, command.getUserId(),
                command.getCommandName(), command.getSlackLink().getBotUserId());
        asyncExecutor.run(() ->
                slackClientProvider.withLink(command.getSlackLink()).postResponse(
                        command.getResponseUrl(),
                        "ephemeral",
                        responseMessage.build()));
    }

    private ChatPostMessageRequestBuilder handleSlashCommand(final String commandText,
                                                             final String slackUserId,
                                                             final String commandName,
                                                             final String botUserId) {
        String response;
        if (commandText.isEmpty() || "help".equalsIgnoreCase(commandText)) {
            response = slackNotificationRenderer.getHelpMessage(botUserId, commandName);
        } else if ("account".equalsIgnoreCase(commandText)) {
            final Optional<Pair<SlackUser, ApplicationUser>> user = findBitbucketAndSlackUser(slackUserId);
            response = slackNotificationRenderer.getAccountMessage(user.map(Pair::getRight).orElse(null));
        } else {
            response = slackNotificationRenderer.getInvalidCommandMessage();
        }

        return ChatPostMessageRequest.builder()
                .text(response)
                .blocks(singletonList(slackNotificationRenderer.richTextSectionBlock(response)));
    }

    private boolean isSupportedSubtype(@Nullable String subtype) {
        return isBlank(subtype)
                || subtype.startsWith("file_comment")
                || subtype.startsWith("file_share")
                || subtype.equals("me_message")
                || subtype.equals("message_replied")
                || subtype.startsWith("thread_broadcast");
    }

    @EventListener
    public void messageReceived(@Nonnull final GenericMessageSlackEvent slackEvent) {
        log.debug("Got message from Slack");

        if (!isSupportedSubtype(slackEvent.getSubtype()) || slackEvent.isHidden()) {
            log.debug("Skipped message unfurl for team {} for message with subtype {} and hidden={}",
                    slackEvent.getSlackEvent().getTeamId(), slackEvent.getSubtype(), slackEvent.isHidden());
            return;
        }

        asyncExecutor.run(() -> {
            String slackUserId = slackEvent.getUser();
            final Optional<Pair<SlackUser, ApplicationUser>> user = findBitbucketAndSlackUser(slackUserId);
            final String channel = slackEvent.getChannel();
            final String messageText = slackEvent.getText();
            final SlackLink slackLink = slackEvent.getSlackEvent().getSlackLink();

            final List<String> links = LinkHelper.extractUrls(messageText).stream()
                    .filter(this::hasBitbucketBaseUrl)
                    .collect(Collectors.toList());

            final boolean shouldUseLinkUnfurl = slackLinkManager.shouldUseLinkUnfurl(slackEvent.getSlackEvent().getTeamId());
            if (!user.isPresent()) {
                if (!links.isEmpty() && !shouldUseLinkUnfurl) {
                    final SlackClient client = slackClientProvider.withLink(slackLink);
                    String message = slackNotificationRenderer.getPleaseAuthenticateMessage();
                    client.postEphemeralMessage(ChatPostEphemeralRequest.builder()
                            .text(message)
                            .blocks(singletonList(slackNotificationRenderer.richTextSectionBlock(message)))
                            .channel(channel)
                            .user(slackUserId)
                            .build());
                }
                return;
            }

            final List<Unfurl> references = unfurlLinkExtractor.findLinksToUnfurl(links, user.get().getRight());

            // direct message to bot without valid page links; show help message
            final boolean isDirectMessage = "im".equals(slackEvent.getChannelType());
            final boolean isMentioningBot = messageText.contains("@" + slackLink.getBotUserId());
            final boolean isThread = isNotBlank(slackEvent.getThreadTimestamp()); // ignore threaded direct message
            if (references.isEmpty() && !isThread && (isDirectMessage || isMentioningBot)) {
                final ChatPostMessageRequestBuilder responseMessage = handleSlashCommand(removeSlackLinks(messageText),
                        slackUserId, null, slackLink.getBotUserId());
                final SlackClient client = slackClientProvider.withLink(slackLink);

                if (isDirectMessage) {
                    client.postMessage(responseMessage.channel(channel).build());
                } else {
                    final ChatPostMessageRequest message = responseMessage.build();

                    client.postEphemeralMessage(ChatPostEphemeralRequest.builder()
                            .text(message.getText())
                            .blocks(message.getBlocks())
                            .user(slackUserId)
                            .channel(slackEvent.getChannel())
                            .build());
                }
                return;
            }

            if (shouldUseLinkUnfurl) {
                log.debug("Skipped message unfurl for team {} since link unfurl is enabled", slackEvent.getSlackEvent().getTeamId());
                return;
            }

            for (Unfurl unfurl : references) {
                final ChatPostMessageRequestBuilder message = ChatPostMessageRequest.builder()
                        .mrkdwn(true)
                        .attachments(Collections.singletonList(unfurl.getAttachment()))
                        .threadTs(slackEvent.getThreadTimestamp());

                AnalyticsContext context = analyticsContextProvider.byTeamIdAndSlackUserId(slackLink.getTeamId(), slackUserId);
                eventPublisher.publish(new BitbucketNotificationSentEvent(context, null, Type.UNFURLING));

                notificationPublisher.sendMessageAsync(slackEvent.getSlackEvent().getTeamId(), channel, message);
            }
        });
    }

    /**
     * Send unfurl when user has confirmed account
     */
    @EventListener
    public void linkShared(@Nonnull final LinkSharedSlackEvent slackEvent) {
        log.debug("Got link shared from Slack");

        if (!slackLinkManager.shouldUseLinkUnfurl(slackEvent.getSlackEvent().getTeamId())) {
            log.debug("Link unfurling disabled for team {} because it is connected as custom installation, not as custom app",
                    slackEvent.getSlackEvent().getTeamId());
            return;
        }

        asyncExecutor.run(() -> {
            String slackUserId = slackEvent.getUser();
            final Optional<Pair<SlackUser, ApplicationUser>> user = findBitbucketAndSlackUser(slackUserId);
            final SlackLink slackLink = slackEvent.getSlackEvent().getSlackLink();
            final SlackClient client = slackClientProvider.withLink(slackLink);
            if (!user.isPresent()) {
                log.debug("User {} shared a link for unfurling without being authenticated. "
                        + "Sending him authentication request", slackUserId);
                client.unfurlWithoutAuthentication(
                        slackEvent.getChannel(),
                        slackEvent.getMessageTimestamp(),
                        slackNotificationRenderer.getPleaseAuthenticateMessage());
                return;
            }

            log.debug("Links before filtering by base URL: {}", slackEvent.getLinks());
            final List<String> links = slackEvent.getLinks().stream()
                    .map(LinkSharedSlackEvent.Link::getUrl)
                    .filter(this::hasBitbucketBaseUrl)
                    .collect(Collectors.toList());

            final List<Unfurl> unfurls = unfurlLinkExtractor.findLinksToUnfurl(links, user.get().getRight());
            if (!unfurls.isEmpty()) {
                final Map<String, Attachment> unfurlsMap = unfurls.stream()
                        .collect(Collectors.toMap(Unfurl::getOriginalUrl, Unfurl::getAttachment));

                AnalyticsContext context = analyticsContextProvider.byTeamIdAndSlackUserId(slackLink.getTeamId(), slackUserId);
                eventPublisher.publish(new BitbucketNotificationSentEvent(context, null, Type.UNFURLING));

                client.withUserTokenIfAvailable(user.get().getLeft()).ifPresent(userClient ->
                        userClient.unfurl(slackEvent.getChannel(), slackEvent.getMessageTimestamp(), unfurlsMap));
            }
        });
    }

    private Optional<Pair<SlackUser, ApplicationUser>> findBitbucketAndSlackUser(final String slackUserId) {
        return slackUserManager.getBySlackUserId(slackUserId)
                .flatMap(slackUser -> Optional
                        .ofNullable(userService.getUserById(Integer.parseInt(slackUser.getUserKey())))
                        .map(user -> Pair.of(slackUser, user)));
    }

    private boolean hasBitbucketBaseUrl(final String link) {
        String baseUrl = applicationProperties.getBaseUrl(UrlMode.CANONICAL);
        return link.startsWith(baseUrl);
    }
}
