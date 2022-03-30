package com.atlassian.plugins.slack.test.mockserver;

import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.SlackUser;
import com.atlassian.plugins.slack.test.TestChannels;
import com.atlassian.plugins.slack.test.TestTeams;
import com.atlassian.plugins.slack.test.TestUsers;
import com.github.seratch.jslack.api.methods.SlackApiErrorResponse;
import com.github.seratch.jslack.api.methods.SlackApiRequest;
import com.github.seratch.jslack.api.methods.SlackApiResponse;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import com.github.seratch.jslack.api.methods.request.chat.ChatUnfurlRequest;
import com.github.seratch.jslack.api.methods.request.conversations.ConversationsInfoRequest;
import com.github.seratch.jslack.api.methods.request.conversations.ConversationsInviteRequest;
import com.github.seratch.jslack.api.methods.request.conversations.ConversationsListRequest;
import com.github.seratch.jslack.api.methods.request.conversations.ConversationsOpenRequest;
import com.github.seratch.jslack.api.methods.request.oauth.OAuthAccessRequest;
import com.github.seratch.jslack.api.methods.request.users.UsersConversationsRequest;
import com.github.seratch.jslack.api.methods.request.users.UsersInfoRequest;
import com.github.seratch.jslack.api.methods.response.auth.AuthTestResponse;
import com.github.seratch.jslack.api.methods.response.chat.ChatPostMessageResponse;
import com.github.seratch.jslack.api.methods.response.chat.ChatUnfurlResponse;
import com.github.seratch.jslack.api.methods.response.conversations.ConversationsInfoResponse;
import com.github.seratch.jslack.api.methods.response.conversations.ConversationsInviteResponse;
import com.github.seratch.jslack.api.methods.response.conversations.ConversationsListResponse;
import com.github.seratch.jslack.api.methods.response.conversations.ConversationsOpenResponse;
import com.github.seratch.jslack.api.methods.response.oauth.OAuthAccessResponse;
import com.github.seratch.jslack.api.methods.response.users.UsersConversationsResponse;
import com.github.seratch.jslack.api.methods.response.users.UsersInfoResponse;
import com.github.seratch.jslack.api.model.Conversation;
import com.github.seratch.jslack.api.model.ConversationType;
import com.github.seratch.jslack.api.model.Message;
import com.github.seratch.jslack.api.model.ResponseMetadata;
import com.github.seratch.jslack.api.model.User;
import com.github.seratch.jslack.common.json.GsonFactory;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.atlassian.plugins.slack.test.TestTeams.DUMMY_TEAM;
import static com.github.seratch.jslack.api.methods.Methods.AUTH_TEST;
import static com.github.seratch.jslack.api.methods.Methods.CHAT_POST_MESSAGE;
import static com.github.seratch.jslack.api.methods.Methods.CHAT_UNFURL;
import static com.github.seratch.jslack.api.methods.Methods.CONVERSATIONS_INFO;
import static com.github.seratch.jslack.api.methods.Methods.CONVERSATIONS_INVITE;
import static com.github.seratch.jslack.api.methods.Methods.CONVERSATIONS_LIST;
import static com.github.seratch.jslack.api.methods.Methods.CONVERSATIONS_OPEN;
import static com.github.seratch.jslack.api.methods.Methods.USERS_CONVERSATIONS;
import static com.github.seratch.jslack.api.methods.Methods.USERS_INFO;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.replaceEachRepeatedly;

@SuppressWarnings("WeakerAccess")
public class SlackMockServer {
    private static final Logger log = LoggerFactory.getLogger(SlackMockServer.class);
    public static final String STATIC_BASE_URL = "http://example.com/context";
    private static final String STATIC_BASE_URL_ENCODED = "http%3A%2F%2Fexample.com%2Fcontext";

    /**
     * list type to fix generic array serialization
     */
    private static final Type type = new TypeToken<List<Object>>() {
    }.getType();

    private final Gson jslackGson = GsonFactory.createSnakeCase();
    private final MockWebServer server;
    private final Map<String, List<RequestHistoryItem>> requestHistory;
    private final Map<String, String> serverBaseUrlsWithEncodedVersion;
    private final AtomicReference<String> testTag;

    public SlackMockServer(final Set<String> serverBaseUrls) {
        this.server = new MockWebServer();
        this.requestHistory = new HashMap<>();
        this.testTag = new AtomicReference<>("");
        this.serverBaseUrlsWithEncodedVersion = serverBaseUrls.stream().collect(Collectors.toMap(
                v -> v,
                v -> {
                    try {
                        return URLEncoder.encode(v, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    }
                }
        ));
    }

    public MockWebServer server() {
        return server;
    }

    public synchronized void setTestTag(final String tag) {
        this.testTag.set(tag);
    }

    private List<RequestHistoryItem> getOrCreateRequestHistoryForTest() {
        return requestHistory.computeIfAbsent(testTag.get(), k -> new ArrayList<>());
    }

    public List<RequestHistoryItem> requestHistoryForTest() {
        synchronized (requestHistory) {
            return getOrCreateRequestHistoryForTest();
        }
    }

    public List<RequestHistoryItem> requestHistoryForTest(final String apiMethod) {
        synchronized (requestHistory) {
            return getOrCreateRequestHistoryForTest().stream()
                    .filter(item -> Objects.equals(apiMethod, item.apiMethod()))
                    .collect(Collectors.toList());
        }
    }

    public void start(final String host, final int port) {
        try {
            log.info("Starting Slack Mock Server at {}:{}", host, port);
            server.setDispatcher(new MockDispatcher());
            server.start(InetAddress.getByName(host), port);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException("Could not start mock server", e);
        }
    }

    public void shutdown() throws IOException {
        log.info("Shutting down Slack Mock Server...");
        server.shutdown();
    }

    public void clearRecords() {
        requestHistory.clear();
    }

    private class MockDispatcher extends Dispatcher {
        @Override
        public MockResponse dispatch(final RecordedRequest request) {
            final String body = request.getBody().readUtf8();
            final RequestHistoryItem historyItem = new RequestHistoryItem(
                    request, replaceRegularAndEncodedBaseUrl(body));
            final boolean isOauthAccess = "oauth.access".equals(historyItem.apiMethod());
            final String reqTag = defaultString(historyItem.tag());

            try {
                // logs untagged request - it shouldn't happen
                if (!isOauthAccess && isBlank(reqTag)) {
                    log.warn("Untagged request: {}", historyItem);
                }

                if (isOauthAccess) {
                    return handleAuthAccess(historyItem);
                }

                // Authentication!!!
                final SlackUser user = TestUsers.BY_TOKEN.get(historyItem.bearerToken());
                final SlackLink team = user != null
                        ? TestTeams.BY_TEAM_ID.get(user.getSlackTeamId())
                        : TestTeams.BY_BOT_TOKEN.get(historyItem.bearerToken());
                if (isBlank(historyItem.bearerToken())) {
                    return error(401, "not_authed");
                }

                if (team == null) {
                    return error(401, "invalid_auth");
                }

                switch (historyItem.apiMethod()) {
                    case AUTH_TEST:
                        return handleAuthTest(team, user);
                    case USERS_CONVERSATIONS:
                        return handleUsersConversations(historyItem);
                    case CONVERSATIONS_LIST:
                        return handleConversationsList(historyItem);
                    case CONVERSATIONS_INFO:
                        return handleConversationsInfo(historyItem);
                    case CONVERSATIONS_OPEN:
                        return handleConversationsOpen(historyItem);
                    case USERS_INFO:
                        return handleUsersInfo(historyItem);
                    case CHAT_POST_MESSAGE:
                        return handlePostMessage(historyItem);
                    case CHAT_UNFURL:
                        return handleUnfurl(historyItem);
                    case CONVERSATIONS_INVITE:
                        return handleConversationsInvite(historyItem);
                    default:
                        return error(400, "invalid_endpoint");
                }
            } finally {
                // check request tag against test tag and save request to history after giving it a chance to parse the request entity
                if (isOauthAccess || Objects.equals(reqTag, testTag.get())) {
                    synchronized (requestHistory) {
                        getOrCreateRequestHistoryForTest().add(historyItem);
                    }
                } else {
                    // log request leaked which shows which tests are leading them
                    log.warn("Detected leaked request with tag '{}': {}", reqTag, historyItem);
                }
            }
        }

        private MockResponse handleConversationsList(final RequestHistoryItem historyItem) {
            ConversationsListRequest input = body(historyItem, ConversationsListRequest.class);
            ConversationsListResponse output = new ConversationsListResponse();
            output.setChannels(filterChannelsByTypes(input.getTypes(), TestChannels.CHANNELS));

            ResponseMetadata meta = new ResponseMetadata();
            meta.setNextCursor("");
            output.setResponseMetadata(meta);
            return success(output);
        }

        private MockResponse handleUsersConversations(final RequestHistoryItem historyItem) {
            UsersConversationsRequest input = body(historyItem, UsersConversationsRequest.class);
            UsersConversationsResponse output = new UsersConversationsResponse();
            output.setChannels(filterChannelsByTypes(input.getTypes(), TestChannels.CHANNELS));

            ResponseMetadata meta = new ResponseMetadata();
            meta.setNextCursor("");
            output.setResponseMetadata(meta);
            return success(output);
        }

        private List<Conversation> filterChannelsByTypes(final List<ConversationType> types, final List<Conversation> channels) {
            boolean includesPublic = types.contains(ConversationType.PUBLIC_CHANNEL);
            boolean includesIm = types.contains(ConversationType.IM);
            boolean includesMpim = types.contains(ConversationType.MPIM);
            boolean includesPrivate = types.contains(ConversationType.PRIVATE_CHANNEL);
            return channels.stream()
                    .filter(c -> (includesIm && c.isIm())
                            || (includesMpim && c.isMpim())
                            || (includesPrivate && c.isPrivate())
                            || (includesPublic && !c.isIm() && !c.isMpim() && !c.isPrivate()))
                    .collect(Collectors.toList());
        }

        private MockResponse handleConversationsInfo(final RequestHistoryItem historyItem) {
            ConversationsInfoRequest input = body(historyItem, ConversationsInfoRequest.class);
            Conversation conversation = TestChannels.BY_ID.get(input.getChannel());
            if (conversation != null) {
                ConversationsInfoResponse output = new ConversationsInfoResponse();
                output.setChannel(conversation);
                return success(output);
            }
            return error(404, "channel_not_found");
        }

        private MockResponse handleConversationsOpen(final RequestHistoryItem historyItem) {
            ConversationsOpenRequest input = body(historyItem, ConversationsOpenRequest.class);
            Conversation conversation = TestChannels.DM_BY_USER_ID.get(new HashSet<>(input.getUsers()));
            if (conversation != null) {
                ConversationsOpenResponse output = new ConversationsOpenResponse();
                output.setChannel(conversation);
                return success(output);
            }
            return error(404, "channel_not_found");
        }

        private MockResponse handleUsersInfo(final RequestHistoryItem historyItem) {
            UsersInfoRequest input = body(historyItem, UsersInfoRequest.class);
            // respond to bot id
            if (Objects.equals(input.getUser(), DUMMY_TEAM.getBotUserId())) {
                User user = new User();
                user.setRealName(DUMMY_TEAM.getBotUserName());
                user.setId(DUMMY_TEAM.getBotUserId());
                user.setTeamId(DUMMY_TEAM.getTeamId());

                User.Profile profile = new User.Profile();
                profile.setApiAppId(DUMMY_TEAM.getAppId());
                user.setProfile(profile);

                UsersInfoResponse output = new UsersInfoResponse();
                output.setUser(user);
                return success(output);
            }
            // respond known user ids
            SlackUser slackUser = TestUsers.BY_ID.get(input.getUser());
            if (slackUser != null) {
                User user = new User();
                user.setRealName(TestUsers.REAL_NAME_BY_ID.get(slackUser.getSlackUserId()));
                user.setId(slackUser.getSlackUserId());
                user.setTeamId(slackUser.getSlackTeamId());

                User.Profile profile = new User.Profile();
                user.setProfile(profile);

                UsersInfoResponse output = new UsersInfoResponse();
                output.setUser(user);
                return success(output);
            }
            return error(404, "user_not_found");
        }

        private MockResponse handleUnfurl(final RequestHistoryItem historyItem) {
            ChatUnfurlRequest input = body(historyItem, ChatUnfurlRequest.class);
            Conversation conversation = TestChannels.BY_ID.get(input.getChannel());
            if (conversation != null) {
                ChatUnfurlResponse output = new ChatUnfurlResponse();
                output.setOk(true);
                return success(output);
            }
            return error(404, "channel_not_found");
        }

        private MockResponse handlePostMessage(final RequestHistoryItem historyItem) {
            ChatPostMessageRequest input = body(historyItem, ChatPostMessageRequest.class);
            Conversation conversation = TestChannels.BY_ID.get(input.getChannel());
            if (conversation != null) {
                ChatPostMessageResponse output = new ChatPostMessageResponse();
                output.setChannel(conversation.getId());
                output.setTs("1503435956.000247");
                Message message = new Message();
                message.setAttachments(input.getAttachments());
                message.setText(input.getText());
                message.setThreadTs(input.getThreadTs());
                message.setBlocks(input.getBlocks());
                output.setMessage(message);
                return success(output);
            }
            return error(404, "channel_not_found");
        }

        private MockResponse handleConversationsInvite(final RequestHistoryItem historyItem) {
            ConversationsInviteRequest input = body(historyItem, ConversationsInviteRequest.class);
            Conversation conversation = TestChannels.BY_ID.get(input.getChannel());
            if (conversation != null) {
                ConversationsInviteResponse output = new ConversationsInviteResponse();
                output.setChannel(conversation);
                return success(output);
            }
            return error(404, "channel_not_found");
        }

        private MockResponse handleAuthTest(final SlackLink team, final SlackUser user) {
            AuthTestResponse output = new AuthTestResponse();
            output.setTeamId(team.getTeamId());
            output.setTeam(team.getTeamName());
            output.setUrl("url." + team.getTeamId());
            output.setUser(user == null ? team.getBotUserName() : TestUsers.REAL_NAME_BY_ID.get(user.getSlackUserId()));
            output.setUserId(user == null ? team.getBotUserId() : user.getSlackUserId());
            return success(output);
        }

        private MockResponse handleAuthAccess(final RequestHistoryItem historyItem) {
            OAuthAccessRequest input = body(historyItem, OAuthAccessRequest.class);

            //test team
            SlackLink team = TestTeams.BY_CLIENT_ID.get(input.getClientId());
            if (team == null) {
                return error(401, "invalid_client_id");
            }

            //test secret
            String clientSecret = defaultString(input.getClientSecret());
            if (!Objects.equals(team.getClientSecret(), clientSecret)) {
                return error(401, "bad_client_secret");
            }

            //test redirect URI
            String redirectUri = defaultString(input.getRedirectUri());
            if (!redirectUri.startsWith(STATIC_BASE_URL)) {
                return error(401, "bad_redirect_uri");
            }

            //test code
            String code = defaultString(input.getCode());
            String[] codeParts = code.split(":");
            if (codeParts.length != 2 || !Objects.equals(codeParts[0], "secret.code")) {
                return error(401, "invalid_code");
            }

            //fetch user
            SlackUser slackUser = TestUsers.BY_ID.get(codeParts[1]);
            if (slackUser == null) {
                return error(401, "invalid_code");
            }

            OAuthAccessResponse output = new OAuthAccessResponse();
            output.setAccessToken(appendTag(slackUser.getUserToken()));
            output.setTeamId(team.getTeamId());
            output.setUserId(slackUser.getSlackUserId());
            return success(output);
        }

        /**
         * Extracts the request body from a json for form data into an object
         */
        private <T extends SlackApiRequest> T body(final RequestHistoryItem historyItem, final Class<T> clazz) {
            if (historyItem.contentType().contains("json")) {
                final T input = jslackGson.fromJson(historyItem.body(), clazz);
                historyItem.setParsedEntity(input);
                return input;
            } else if (historyItem.contentType().contains("form")) {
                // convert form data into a map,, considering multiple values
                final Map<String, List<String>> fields = Stream.of(historyItem.body().split("&"))
                        .map(field -> {
                            final String[] nameAndValue = field.split("=");
                            try {
                                final String name = URLDecoder.decode(nameAndValue[0], StandardCharsets.UTF_8.name());
                                final String value = nameAndValue.length > 1
                                        ? URLDecoder.decode(nameAndValue[1], StandardCharsets.UTF_8.name())
                                        : "";
                                return Pair.of(name, value);
                            } catch (UnsupportedEncodingException e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .collect(groupingBy(Pair::getLeft, mapping(Pair::getRight, Collectors.toList())));

                // fix fields that are objects or arrays so they're correctly mapped later
                final Map<String, Object> fieldsFixed = fields.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> {
                    // look up the getter method of the target class type to identify the expected field type
                    final String methodName = Stream
                            .of(e.getKey().split("_"))
                            .map(StringUtils::capitalize)
                            .collect(Collectors.joining());
                    final Method getterMethod = ReflectionUtils.findMethod(clazz, "get" + methodName);
                    if (getterMethod != null) {
                        final Class<?> returnType = getterMethod.getReturnType();
                        final boolean isNotPrimitiveOrString = !returnType.isPrimitive() && !String.class.isAssignableFrom(returnType);
                        if (isNotPrimitiveOrString) {
                            if (e.getValue().size() > 1) {
                                return e.getValue();
                            }

                            final String value = e.getValue().get(0);
                            final boolean isValueObjectOrArray = looksLikeJsonObjectOrArray(value);

                            // Some values sent as form fields are comma-separated strings to be mapped as arrays, such
                            // as channel types: {@code "types=public_channel,private_channel,im,mpim"}.
                            final boolean isExpectedTypeArrayOrList = returnType.isArray() || Iterable.class.isAssignableFrom(returnType);
                            if (isExpectedTypeArrayOrList && !isValueObjectOrArray) {
                                return Arrays.asList(e.getValue().get(0).split(","));
                            }

                            // if field content looks like a json object or array, let's unpack it
                            return isValueObjectOrArray ? jslackGson.fromJson(value, type) : value;
                        }
                    }

                    return e.getValue().get(0);
                }));

                // convert the map into a json and a json into the actual expected object
                final T input = jslackGson.fromJson(jslackGson.toJson(fieldsFixed), clazz);
                historyItem.setParsedEntity(input);
                return input;
            }
            return null;
        }

        private MockResponse success(final SlackApiResponse response) {
            response.setOk(true);
            return response(response, 200);
        }

        private MockResponse error(final int code, final String message) {
            SlackApiErrorResponse response = new SlackApiErrorResponse();
            response.setOk(false);
            response.setError(message);
            return response(response, code);
        }

        private MockResponse response(final SlackApiResponse response, final int code) {
            return new MockResponse()
                    .setResponseCode(code)
                    .setBody(new Buffer().writeString(jslackGson.toJson(response), StandardCharsets.UTF_8));
        }

        /**
         * Replaces any occurrence of the base URL in the body text so we can assert for requests to Slack without
         * having to be aware of the actual base URL. It also replaces URL encoded version in order to be compatible
         * with form version.
         */
        private String replaceRegularAndEncodedBaseUrl(String bodyPiece) {
            for (Map.Entry<String, String> baseUrlWithEncodedVersion : serverBaseUrlsWithEncodedVersion.entrySet()) {
                bodyPiece = replaceEachRepeatedly(bodyPiece,
                        new String[]{baseUrlWithEncodedVersion.getKey(), baseUrlWithEncodedVersion.getValue()},
                        new String[]{STATIC_BASE_URL_ENCODED, STATIC_BASE_URL});
            }
            return bodyPiece;
        }

        private boolean looksLikeJsonObjectOrArray(final String value) {
            return value.startsWith("[") || value.startsWith("{");
        }
    }

    private String appendTag(final String token) {
        return token + (isNotBlank(testTag.get()) ? "#" + testTag.get() : "");

    }
}
