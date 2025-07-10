package com.atlassian.plugins.slack.api.client;

import com.atlassian.annotations.VisibleForTesting;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugins.slack.api.DelayedSlackMessage;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.SlackUser;
import com.atlassian.plugins.slack.api.UserNotLinkedException;
import com.atlassian.plugins.slack.api.client.cache.SlackResponseCache;
import com.atlassian.plugins.slack.api.events.SlackConversationsLoadedEvent;
import com.atlassian.plugins.slack.api.webhooks.ChannelArchiveSlackEvent;
import com.atlassian.plugins.slack.api.webhooks.SlackEvent;
import com.atlassian.plugins.slack.user.SlackUserManager;
import com.atlassian.plugins.slack.util.ErrorResponse;
import com.atlassian.sal.api.user.UserManager;
import com.github.seratch.jslack.Slack;
import com.github.seratch.jslack.SlackConfig;
import com.github.seratch.jslack.api.methods.MethodsClient;
import com.github.seratch.jslack.api.methods.request.auth.AuthTestRequest;
import com.github.seratch.jslack.api.methods.request.chat.ChatGetPermalinkRequest;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostEphemeralRequest;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import com.github.seratch.jslack.api.methods.request.chat.ChatUnfurlRequest;
import com.github.seratch.jslack.api.methods.request.conversations.ConversationsCreateRequest;
import com.github.seratch.jslack.api.methods.request.conversations.ConversationsInfoRequest;
import com.github.seratch.jslack.api.methods.request.conversations.ConversationsInviteRequest;
import com.github.seratch.jslack.api.methods.request.conversations.ConversationsListRequest;
import com.github.seratch.jslack.api.methods.request.conversations.ConversationsOpenRequest;
import com.github.seratch.jslack.api.methods.request.conversations.ConversationsSetTopicRequest;
import com.github.seratch.jslack.api.methods.request.dialog.DialogOpenRequest;
import com.github.seratch.jslack.api.methods.request.oauth.OAuthAccessRequest;
import com.github.seratch.jslack.api.methods.request.users.UsersConversationsRequest;
import com.github.seratch.jslack.api.methods.request.users.UsersInfoRequest;
import com.github.seratch.jslack.api.methods.response.auth.AuthTestResponse;
import com.github.seratch.jslack.api.methods.response.chat.ChatGetPermalinkResponse;
import com.github.seratch.jslack.api.methods.response.chat.ChatPostEphemeralResponse;
import com.github.seratch.jslack.api.methods.response.chat.ChatPostMessageResponse;
import com.github.seratch.jslack.api.methods.response.chat.ChatUnfurlResponse;
import com.github.seratch.jslack.api.methods.response.conversations.ConversationsCreateResponse;
import com.github.seratch.jslack.api.methods.response.conversations.ConversationsInfoResponse;
import com.github.seratch.jslack.api.methods.response.conversations.ConversationsInviteResponse;
import com.github.seratch.jslack.api.methods.response.conversations.ConversationsOpenResponse;
import com.github.seratch.jslack.api.methods.response.conversations.ConversationsSetTopicResponse;
import com.github.seratch.jslack.api.methods.response.dialog.DialogOpenResponse;
import com.github.seratch.jslack.api.methods.response.oauth.OAuthAccessResponse;
import com.github.seratch.jslack.api.methods.response.users.UsersInfoResponse;
import com.github.seratch.jslack.api.model.Attachment;
import com.github.seratch.jslack.api.model.Conversation;
import com.github.seratch.jslack.api.model.ConversationType;
import com.github.seratch.jslack.api.model.Message;
import com.github.seratch.jslack.api.model.User;
import com.github.seratch.jslack.api.model.block.LayoutBlock;
import com.github.seratch.jslack.api.model.dialog.Dialog;
import com.github.seratch.jslack.common.http.SlackHttpClient;
import com.github.seratch.jslack.common.json.GsonFactory;
import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import io.atlassian.fugue.Either;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.atlassian.plugins.slack.util.ResponseMapper.responseToEither;
import static com.atlassian.plugins.slack.util.ResponseMapper.toEither;
import static com.github.seratch.jslack.api.methods.Methods.CONVERSATIONS_INFO;
import static com.github.seratch.jslack.api.methods.Methods.CONVERSATIONS_OPEN;
import static com.github.seratch.jslack.api.methods.Methods.USERS_INFO;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

@Slf4j
public class DefaultSlackClient implements SlackClient {
    public static final String TEAM_DUMMY_PREFIX = "DUMMY-";
    public static final String SLACK_MOCK_SERVER_SYSTEM_PROPERTY = "slack.mock.server";
    public static final String SLACK_MOCK_SERVER_DEFAULT_BASE_URL = "http://localhost:9753/";

    public static final String SLACK_CLIENT_LIST_ALL_CONVERSATIONS_SYSTEM_PROPERTY = "slack.client.list.all.conversations";
    public static final String SLACK_CLIENT_PAGINATION_LIMIT_SYSTEM_PROPERTY = "slack.client.pagination.limit";
    private static final int CHANNELS_PER_PAGE = 1000;
    private static final int DEFAULT_MAX_PAGE_NUMBER = 2; // i.e. 2 thousand channels at most

    private final Gson gson = GsonFactory.createSnakeCase();

    private final SlackHttpClient slackHttpClient;
    private final Slack slack;
    private final MethodsClient slackMethods;
    private final SlackLink slackLink;
    private final SlackUser slackUser;
    private final SlackUserManager slackUserManager;
    private final UserManager userManager;
    private final EventPublisher eventPublisher;
    private final SlackResponseCache slackResponseCache;
    private final String currentToken;
    private final int maxPagesToFetchFromSlack;
    private final boolean shouldListAllConversations;

    DefaultSlackClient(final SlackHttpClient slackHttpClient,
                       final SlackLink slackLink,
                       final SlackUserManager slackUserManager,
                       final UserManager userManager,
                       final EventPublisher eventPublisher,
                       final SlackResponseCache slackResponseCache) {
        this(
                slackHttpClient,
                Slack.getInstance(slackHttpClient),
                slackLink,
                null,
                slackUserManager,
                userManager,
                eventPublisher,
                slackResponseCache,
                slackLink.getBotAccessToken());
    }

    @VisibleForTesting
    DefaultSlackClient(final SlackHttpClient slackHttpClient,
                       final Slack slack,
                       final SlackLink slackLink,
                       final SlackUserManager slackUserManager,
                       final UserManager userManager,
                       final EventPublisher eventPublisher,
                       final SlackResponseCache slackResponseCache) {
        this(
                slackHttpClient,
                slack,
                slackLink,
                null,
                slackUserManager,
                userManager,
                eventPublisher,
                slackResponseCache,
                slackLink.getBotAccessToken());
    }

    private DefaultSlackClient(final SlackHttpClient slackHttpClient,
                               final Slack slack,
                               final SlackLink slackLink,
                               @Nullable final SlackUser slackUser,
                               final SlackUserManager slackUserManager,
                               final UserManager userManager,
                               final EventPublisher eventPublisher,
                               final SlackResponseCache slackResponseCache,
                               final String currentToken) {
        this.slackHttpClient = slackHttpClient;
        this.slackLink = slackLink;
        this.slackUser = slackUser;
        this.slackUserManager = slackUserManager;
        this.userManager = userManager;
        this.eventPublisher = eventPublisher;
        this.slackResponseCache = slackResponseCache;
        this.currentToken = currentToken;
        this.slackMethods = slack.methods(); //every call creates a new object, so we better cache it

        // enables integration tests support
        // in case of custom configuration creating teamId is empty
        String teamId = trimToEmpty(slackLink.getTeamId());
        String clientId = trimToEmpty(slackLink.getClientId());
        if (teamId.startsWith(TEAM_DUMMY_PREFIX) || clientId.startsWith(TEAM_DUMMY_PREFIX)) {
            final String slackMockBaseUrl = System.getProperty(
                    SLACK_MOCK_SERVER_SYSTEM_PROPERTY,
                    SLACK_MOCK_SERVER_DEFAULT_BASE_URL);
            log.info("Dummy client created slack mock server at {}", slackMockBaseUrl);
            slackMethods.setEndpointUrlPrefix(slackMockBaseUrl);

            final SlackConfig slackConfig = new SlackConfig();
            slackConfig.setPrettyResponseLoggingEnabled(true);
            this.slack = Slack.getInstance(slackConfig, slackHttpClient);
        } else {
            this.slack = slack;
        }

        this.shouldListAllConversations = Boolean.getBoolean(SLACK_CLIENT_LIST_ALL_CONVERSATIONS_SYSTEM_PROPERTY);
        this.maxPagesToFetchFromSlack = Integer.getInteger(SLACK_CLIENT_PAGINATION_LIMIT_SYSTEM_PROPERTY, DEFAULT_MAX_PAGE_NUMBER);
    }

    private static final List<ConversationType> PUBLIC_AND_PRIVATE = Arrays.asList(
            ConversationType.PUBLIC_CHANNEL,
            ConversationType.PRIVATE_CHANNEL);

    @Override
    public Optional<SlackClient> withUserTokenIfAvailable(final SlackUser slackUser) {
        checkNotNull(slackUser, "slackUser cannot be null");
        if (isBlank(slackUser.getUserToken())) {
            return Optional.empty();
        }
        return Optional.of(new DefaultSlackClient(
                slackHttpClient,
                slack,
                slackLink,
                slackUser,
                slackUserManager,
                userManager,
                eventPublisher,
                slackResponseCache,
                slackUser.getUserToken()));
    }

    @Override
    public Optional<SlackClient> withUserTokenIfAvailable(final String userKey) {
        return Optional.ofNullable(userKey)
                .flatMap(key -> slackUserManager.getByTeamIdAndUserKey(slackLink.getTeamId(), key))
                .flatMap(this::withUserTokenIfAvailable);
    }

    @Override
    public Either<UserNotLinkedException, SlackClient> withUserToken(final String userKey) {
        return withUserTokenIfAvailable(userKey)
                .map(Either::<UserNotLinkedException, SlackClient>right)
                .orElseGet(() -> Either.left(new UserNotLinkedException(userKey)));
    }

    @Override
    public Optional<SlackClient> withRemoteUserTokenIfAvailable() {
        return Optional.ofNullable(userManager.getRemoteUser())
                .flatMap(user -> withUserTokenIfAvailable(user.getUserKey().getStringValue()));
    }

    @Override
    public Either<UserNotLinkedException, SlackClient> withRemoteUser() {
        return withRemoteUserTokenIfAvailable()
                .map(Either::<UserNotLinkedException, SlackClient>right)
                .orElseGet(() -> Either.left(
                        new UserNotLinkedException(userManager.getRemoteUser().getUserKey().getStringValue())));
    }

    @Override
    public SlackClient withInstallerUserToken() {
        return new DefaultSlackClient(
                slackHttpClient,
                slack,
                slackLink,
                slackUser,
                slackUserManager,
                userManager,
                eventPublisher,
                slackResponseCache,
                slackLink.getAccessToken());
    }

    @Override
    public SlackLink getLink() {
        return slackLink;
    }

    @Override
    @Nonnull
    public Optional<SlackUser> getUser() {
        return Optional.ofNullable(slackUser);
    }

    private boolean hasUserToken() {
        return slackUser != null && isNotBlank(slackUser.getUserToken());
    }

    // token

    @Override
    public Either<ErrorResponse, AuthTestResponse> testToken() {
        // https://api.slack.com/methods/auth.test
        return toEither("auth.test/" + (hasUserToken() ? "user" : "bot"), () ->
                slackMethods.authTest(AuthTestRequest.builder()
                        .token(currentToken)
                        .build()));
    }

    @Override
    public Either<ErrorResponse, OAuthAccessResponse> getOauthAccessToken(final String code, final String redirectUri) {
        Preconditions.checkNotNull(slackLink, "slackLink was not provided");
        checkArgument(isNotBlank(code), "code cannot be null or empty");

        // https://api.slack.com/methods/oauth.access
        Either<ErrorResponse, OAuthAccessResponse> result = toEither("oauth.access/" + slackLink.getClientId(), () ->
                slackMethods.oauthAccess(OAuthAccessRequest.builder()
                        .code(code)
                        .redirectUri(redirectUri)
                        .clientId(slackLink.getClientId())
                        .clientSecret(slackLink.getClientSecret())
                        .build()));
        return result;
    }

    // conversations

    private Either<ErrorResponse, List<Conversation>> listConversations(final String cursor,
                                                                        final int n) {
        if (n > 1 && isBlank(cursor) || n > maxPagesToFetchFromSlack) {
            return Either.right(Collections.emptyList());
        }

        // https://api.slack.com/methods/conversations.list
        return toEither(
                "conversations.list/" + slackLink.getTeamId(),
                () -> slackMethods.conversationsList(ConversationsListRequest.builder()
                        .token(currentToken)
                        .cursor(cursor)
                        .types(PUBLIC_AND_PRIVATE)
                        .excludeArchived(true)
                        .limit(CHANNELS_PER_PAGE)
                        .build())
        ).flatMap(resp -> listConversations(resp.getResponseMetadata().getNextCursor(), n + 1)
                .map(innerResp -> Stream
                        .concat(resp.getChannels().stream().filter(c -> !c.isReadOnly()), innerResp.stream())
                        .collect(Collectors.toList())));
    }

    private Either<ErrorResponse, List<Conversation>> listUserConversations(final String cursor, final int n) {
        if (n > 1 && isBlank(cursor) || n > maxPagesToFetchFromSlack) {
            return Either.right(Collections.emptyList());
        }

        // https://api.slack.com/methods/users.conversations
        return toEither(
                "users.conversations/" + slackUser.getSlackUserId(),
                () -> slackMethods.usersConversations(UsersConversationsRequest.builder()
                        .token(currentToken)
                        .cursor(cursor)
                        .types(PUBLIC_AND_PRIVATE)
                        .excludeArchived(true)
                        .limit(CHANNELS_PER_PAGE)
                        .build())
        ).flatMap(resp -> listUserConversations(resp.getResponseMetadata().getNextCursor(), n + 1)
                .map(innerResp -> Stream
                        .concat(resp.getChannels().stream().filter(c -> !c.isReadOnly()), innerResp.stream())
                        .collect(Collectors.toList())));
    }

    @Override
    public Either<ErrorResponse, List<Conversation>> getAllConversations() {
        checkArgument(hasUserToken(), "requires a user token");

        final Either<ErrorResponse, List<Conversation>> result = shouldListAllConversations
                ? listConversations(null, 1)
                : listUserConversations(null, 1);

        return result.map(conversations -> {
            // check 'is_archived' flag and unmute mappings
            eventPublisher.publish(new SlackConversationsLoadedEvent(conversations, slackLink.getTeamId()));
            return conversations;
        });
    }

    private ConversationsInfoResponse getConversationsInfoCached(final String conversationId) throws Exception {
        return slackResponseCache.getAndCacheIfSuccessful(
                currentToken,
                CONVERSATIONS_INFO,
                conversationId,
                () -> slackMethods.conversationsInfo(ConversationsInfoRequest.builder()
                        .token(currentToken)
                        .channel(conversationId)
                        .build()),
                ConversationsInfoResponse.class);
    }

    @Override
    public Either<ErrorResponse, Conversation> getConversationsInfo(final String conversationId) {
        checkConversation(conversationId);

        // https://api.slack.com/methods/conversations.info
        return toEither(
                "conversations.info/" + conversationId,
                () -> getConversationsInfoCached(conversationId)
        ).map(conversationsInfoResponse -> {
            Conversation channel = conversationsInfoResponse.getChannel();
            if (channel.isArchived()) {
                triggerChannelArchivedEvent(conversationId);
            }
            return channel;
        });
    }

    @Override
    public Either<ErrorResponse, Conversation> createConversation(final String name) {
        checkArgument(isNotBlank(name), "conversation name cannot be null or empty");

        // https://api.slack.com/methods/conversations.create
        return toEither(
                "conversations.create",
                () -> slackMethods.conversationsCreate(ConversationsCreateRequest.builder()
                        .token(currentToken)
                        .name(name)
                        .isPrivate(false)
                        .build())
        ).map(ConversationsCreateResponse::getChannel);
    }

    @Override
    public Either<ErrorResponse, Conversation> setConversationTopic(final String conversationId, final String topic) {
        checkConversation(conversationId);
        checkArgument(isNotBlank(topic), "conversation topic cannot be null or empty");

        // https://api.slack.com/methods/conversations.setTopic
        return toEither(
                "conversations.setTopic/" + conversationId,
                () -> slackMethods.conversationsSetTopic(ConversationsSetTopicRequest
                        .builder()
                        .token(currentToken)
                        .channel(conversationId)
                        .topic(topic)
                        .build())
        ).map(ConversationsSetTopicResponse::getChannel);
    }

    @Override
    public Either<ErrorResponse, Conversation> selfInviteToConversation(final String conversationId) {
        checkConversation(conversationId);

        // https://api.slack.com/methods/conversations.invite
        return toEither(
                "conversations.invite/" + conversationId,
                () -> slackMethods.conversationsInvite(ConversationsInviteRequest.builder()
                        .token(currentToken)
                        .users(Collections.singletonList(slackLink.getBotUserId()))
                        .channel(conversationId)
                        .build())
        ).map(ConversationsInviteResponse::getChannel);
    }

    // users

    private UsersInfoResponse getUsersInfoCached(final String slackUserId) throws Exception {
        return slackResponseCache.getAndCacheIfSuccessful(
                "bot",
                USERS_INFO,
                slackUserId,
                () -> slackMethods.usersInfo(UsersInfoRequest.builder()
                        .token(slackLink.getBotAccessToken())
                        .user(slackUserId)
                        .build()),
                UsersInfoResponse.class);
    }

    @Override
    public Either<ErrorResponse, User> getUserInfo(final String slackUserId) {
        checkArgument(isNotBlank(slackUserId), "slackUserId cannot be null or empty");

        // https://api.slack.com/methods/users.info
        return toEither(
                "users.info/" + slackUserId,
                () -> getUsersInfoCached(slackUserId)
        ).map(UsersInfoResponse::getUser);
    }

    // messages

    private void handleMessageError(final ErrorResponse error,
                                    @Nullable final List<LayoutBlock> blocks,
                                    @Nullable final String channelId) {
        if (channelId != null && "is_archived".equals(error.getMessage())) {
            triggerChannelArchivedEvent(channelId);
        }
        if (blocks != null && "invalid_blocks".equals(error.getMessage())) {
            log.info("Slack rejected the following blocks: {}", blocks);
        }
    }

    @Override
    public Either<ErrorResponse, Message> postMessage(final ChatPostMessageRequest messageRequest) {
        Preconditions.checkNotNull(messageRequest, "messageRequest cannot be null");

        final String channelId = messageRequest.getChannel();
        final ChatPostMessageRequest tokenizedMessageRequest = ChatPostMessageRequest.builder()
                .attachments(messageRequest.getAttachments())
                .channel(channelId)
                .token(currentToken)
                .parse(messageRequest.getParse())
                .text(messageRequest.getText())
                .asUser(messageRequest.isAsUser())
                .iconEmoji(messageRequest.getIconEmoji())
                .iconUrl(messageRequest.getIconUrl())
                .linkNames(messageRequest.isLinkNames())
                .mrkdwn(messageRequest.isMrkdwn())
                .replyBroadcast(messageRequest.isReplyBroadcast())
                .threadTs(messageRequest.getThreadTs())
                .unfurlLinks(messageRequest.isUnfurlLinks())
                .unfurlMedia(messageRequest.isUnfurlMedia())
                .username(messageRequest.getUsername())
                .blocks(messageRequest.getBlocks())
                .build();

        // https://api.slack.com/methods/chat.postMessage
        return toEither(
                "chat.postMessage/" + channelId,
                () -> slackMethods.chatPostMessage(tokenizedMessageRequest)
        ).map(ChatPostMessageResponse::getMessage)
                .leftMap(error -> {
                    handleMessageError(error, messageRequest.getBlocks(), channelId);
                    return error;
                });
    }

    @Override
    public Either<ErrorResponse, String> postEphemeralMessage(final ChatPostEphemeralRequest messageRequest) {
        Preconditions.checkNotNull(messageRequest, "messageRequest cannot be null");

        final String channelId = messageRequest.getChannel();
        final ChatPostEphemeralRequest tokenizedMessageRequest = ChatPostEphemeralRequest.builder()
                .attachments(messageRequest.getAttachments())
                .channel(channelId)
                .token(currentToken)
                .parse(messageRequest.getParse())
                .text(messageRequest.getText())
                .user(messageRequest.getUser())
                .asUser(messageRequest.isAsUser())
                .linkNames(messageRequest.isLinkNames())
                .blocks(messageRequest.getBlocks())
                .build();

        // https://api.slack.com/methods/chat.postMessage
        return toEither(
                "chat.postEphemeral/" + channelId + "/" + messageRequest.getUser(),
                () -> slackMethods.chatPostEphemeral(tokenizedMessageRequest)
        )
                .leftMap(error -> {
                    handleMessageError(error, messageRequest.getBlocks(), channelId);
                    return error;
                })
                .map(ChatPostEphemeralResponse::getMessageTs);
    }

    @Override
    public Either<ErrorResponse, Boolean> postResponse(final String responseUrl,
                                                       final String responseType,
                                                       final ChatPostMessageRequest messageRequest) {
        checkArgument(isNotBlank(responseUrl), "responseUrl cannot be null or empty");
        checkArgument(isNotBlank(responseType), "responseType cannot be null or empty");
        Preconditions.checkNotNull(messageRequest, "messageRequest cannot be null");

        final DelayedSlackMessage delayedSlackMessage = new DelayedSlackMessage();
        delayedSlackMessage.setText(messageRequest.getText());
        delayedSlackMessage.setAttachments(messageRequest.getAttachments());
        delayedSlackMessage.setBlocks(messageRequest.getBlocks());
        delayedSlackMessage.setResponseType(responseType);

        /// https://api.slack.com/slash-commands#responding_response_url
        return responseToEither(
                "post.response.message",
                () -> slackHttpClient.postJsonPostRequest(responseUrl, delayedSlackMessage)
        )
                .leftMap(error -> {
                    handleMessageError(error, messageRequest.getBlocks(), null);
                    return error;
                })
                .map(code -> true);
    }

    private ConversationsOpenResponse getConversationsOpenCached(final String slackUserId) throws Exception {
        return slackResponseCache.getAndCacheIfSuccessful(
                "bot",
                CONVERSATIONS_OPEN,
                slackUserId,
                () -> slackMethods.conversationsOpen(ConversationsOpenRequest.builder()
                        .token(slackLink.getBotAccessToken())
                        .users(Collections.singletonList(slackUserId))
                        .build()),
                ConversationsOpenResponse.class);
    }

    @Override
    public Either<ErrorResponse, Message> postDirectMessage(final String slackUserId,
                                                            final ChatPostMessageRequest messageRequest) {
        checkArgument(isNotBlank(slackUserId), "slackUserId cannot be null or empty");
        Preconditions.checkNotNull(messageRequest, "messageRequest cannot be null");

        // https://api.slack.com/methods/conversations.open
        return toEither("conversations.open/" + slackUserId,
                () -> getConversationsOpenCached(slackUserId)
        ).flatMap(resp -> {
            final String userChannelId = resp.getChannel().getId();
            final ChatPostMessageRequest tokenizedMessageRequest = ChatPostMessageRequest.builder()
                    .attachments(messageRequest.getAttachments())
                    .channel(userChannelId)
                    .token(slackLink.getBotAccessToken())
                    .parse(messageRequest.getParse())
                    .text(messageRequest.getText())
                    .asUser(messageRequest.isAsUser())
                    .iconEmoji(messageRequest.getIconEmoji())
                    .iconUrl(messageRequest.getIconUrl())
                    .linkNames(messageRequest.isLinkNames())
                    .mrkdwn(messageRequest.isMrkdwn())
                    .replyBroadcast(messageRequest.isReplyBroadcast())
                    .threadTs(messageRequest.getThreadTs())
                    .unfurlLinks(messageRequest.isUnfurlLinks())
                    .unfurlMedia(messageRequest.isUnfurlMedia())
                    .username(messageRequest.getUsername())
                    .blocks(messageRequest.getBlocks())
                    .build();

            // https://api.slack.com/methods/chat.postMessage
            return toEither(
                    "chat.postMessage/" + slackUserId + "/" + userChannelId,
                    () -> slackMethods.chatPostMessage(tokenizedMessageRequest)
            )
                    .leftMap(error -> {
                        handleMessageError(error, messageRequest.getBlocks(), userChannelId);
                        return error;
                    })
                    .map(ChatPostMessageResponse::getMessage);
        });
    }

    @Override
    public Either<ErrorResponse, Boolean> unfurl(final String conversationId,
                                                 final String messageTimestamp,
                                                 final Map<String, Attachment> unfurls) {
        checkConversation(conversationId);
        checkArgument(isNotBlank(messageTimestamp), "messageTimestamp cannot be null or empty");

        final String token = hasUserToken() ? currentToken : slackLink.getAccessToken();

        try {
            final String unfurlsStr = gson.toJson(unfurls);

            // https://api.slack.com/methods/chat.unfurl
            return toEither(
                    "chat.unfurl/" + conversationId,
                    () -> slackMethods.chatUnfurl(ChatUnfurlRequest.builder()
                            .token(token)
                            .channel(conversationId)
                            .ts(messageTimestamp)
                            .unfurls(unfurlsStr)
                            .build())
            ).map(ChatUnfurlResponse::isOk);
        } catch (RuntimeException e) {
            return Either.left(new ErrorResponse(e));
        }
    }

    @Override
    public Either<ErrorResponse, Boolean> unfurlWithoutAuthentication(final String conversationId,
                                                                      final String messageTimestamp,
                                                                      final String oAuthErrorMessage) {
        checkConversation(conversationId);
        checkArgument(isNotBlank(messageTimestamp), "messageTimestamp cannot be null or empty");
        checkArgument(isNotBlank(oAuthErrorMessage), "oAuthErrorMessage cannot be null or empty");

        // https://api.slack.com/methods/chat.unfurl
        return toEither(
                "chat.unfurl/" + conversationId,
                () -> slackMethods.chatUnfurl(ChatUnfurlRequest.builder()
                        .token(slackLink.getAccessToken())
                        .userAuthMessage(oAuthErrorMessage)
                        .userAuthRequired(true)
                        .channel(conversationId)
                        .ts(messageTimestamp)
                        .build())
        ).map(ChatUnfurlResponse::isOk);
    }

    @Override
    public Either<ErrorResponse, String> getPermalink(final String conversationId, final String messageTimestamp) {
        checkConversation(conversationId);
        checkArgument(isNotBlank(messageTimestamp), "messageTimestamp cannot be null or empty");

        // https://api.slack.com/methods/chat.getPermalink
        return toEither("chat.getPermalink/" + conversationId, () ->
                slackMethods.chatGetPermalink(ChatGetPermalinkRequest.builder()
                        .token(slackLink.getBotAccessToken())
                        .channel(conversationId)
                        .messageTs(messageTimestamp)
                        .build())
        ).map(ChatGetPermalinkResponse::getPermalink);
    }

    @Override
    public Either<ErrorResponse, Boolean> dialogOpen(final String triggerId, final Dialog dialog) {
        // https://api.slack.com/methods/dialog.open
        return toEither("dialog.open/" + triggerId, () ->
                slackMethods.dialogOpen(DialogOpenRequest.builder()
                        .token(slackLink.getBotAccessToken())
                        .triggerId(triggerId)
                        .dialog(dialog)
                        .build()))
                .map(DialogOpenResponse::isOk);
    }

    private void checkConversation(String conversationId) {
        checkArgument(isNotBlank(conversationId), "conversationId cannot be null or empty");
    }

    private void triggerChannelArchivedEvent(final String channelId) {
        ChannelArchiveSlackEvent event = new ChannelArchiveSlackEvent();
        event.setChannel(channelId);
        event.setSlackEvent(new SlackEvent(slackLink.getTeamId(), null, 0, slackLink));
        eventPublisher.publish(event);
    }
}
