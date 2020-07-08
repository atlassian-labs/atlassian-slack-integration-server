package com.atlassian.plugins.slack.api.client;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugins.slack.api.DelayedSlackMessage;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.SlackUser;
import com.atlassian.plugins.slack.api.client.cache.SlackResponseCache;
import com.atlassian.plugins.slack.user.SlackUserManager;
import com.atlassian.plugins.slack.util.ErrorResponse;
import com.atlassian.plugins.slack.util.ResponseSupplier;
import com.atlassian.sal.api.user.UserKey;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import com.github.seratch.jslack.Slack;
import com.github.seratch.jslack.api.methods.MethodsClient;
import com.github.seratch.jslack.api.methods.request.auth.AuthTestRequest;
import com.github.seratch.jslack.api.methods.request.chat.ChatGetPermalinkRequest;
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
import com.github.seratch.jslack.api.methods.response.chat.ChatPostMessageResponse;
import com.github.seratch.jslack.api.methods.response.chat.ChatUnfurlResponse;
import com.github.seratch.jslack.api.methods.response.conversations.ConversationsCreateResponse;
import com.github.seratch.jslack.api.methods.response.conversations.ConversationsInfoResponse;
import com.github.seratch.jslack.api.methods.response.conversations.ConversationsInviteResponse;
import com.github.seratch.jslack.api.methods.response.conversations.ConversationsListResponse;
import com.github.seratch.jslack.api.methods.response.conversations.ConversationsOpenResponse;
import com.github.seratch.jslack.api.methods.response.conversations.ConversationsSetTopicResponse;
import com.github.seratch.jslack.api.methods.response.dialog.DialogOpenResponse;
import com.github.seratch.jslack.api.methods.response.oauth.OAuthAccessResponse;
import com.github.seratch.jslack.api.methods.response.users.UsersConversationsResponse;
import com.github.seratch.jslack.api.methods.response.users.UsersInfoResponse;
import com.github.seratch.jslack.api.model.Attachment;
import com.github.seratch.jslack.api.model.Conversation;
import com.github.seratch.jslack.api.model.ConversationType;
import com.github.seratch.jslack.api.model.Message;
import com.github.seratch.jslack.api.model.ResponseMetadata;
import com.github.seratch.jslack.api.model.User;
import com.github.seratch.jslack.api.model.block.SectionBlock;
import com.github.seratch.jslack.api.model.dialog.Dialog;
import com.github.seratch.jslack.common.http.SlackHttpClient;
import com.github.seratch.jslack.common.json.GsonFactory;
import com.google.common.collect.ImmutableMap;
import io.atlassian.fugue.Either;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.atlassian.plugins.slack.api.client.DefaultSlackClient.SLACK_CLIENT_LIST_ALL_CONVERSATIONS_SYSTEM_PROPERTY;
import static com.github.seratch.jslack.api.methods.Methods.CONVERSATIONS_INFO;
import static com.github.seratch.jslack.api.methods.Methods.CONVERSATIONS_OPEN;
import static com.github.seratch.jslack.api.methods.Methods.USERS_INFO;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultSlackClientTest {
    private static final String TEAM_ID = "someTeamId";
    private static final String USER_TOKEN = "someUserToken";
    private static final String BOT_TOKEN = "someBotToken";
    private static final String CLIENT_ID = "someClientId";
    private static final String CLIENT_SECRET = "someClientSecret";

    @Mock
    private SlackHttpClient slackHttpClient;
    @Mock
    private Slack slack;
    @Mock
    private SlackLink slackLink;
    @Mock
    private SlackUser slackUser;
    @Mock
    private SlackUserManager slackUserManager;
    @Mock
    private UserManager userManager;
    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private SlackResponseCache slackResponseCache;
    @Mock
    private MethodsClient methods;
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private SlackClient client;

    @Before
    public void setUp() {
        when(slackLink.getTeamId()).thenReturn(TEAM_ID);
        when(slackLink.getClientId()).thenReturn(CLIENT_ID);
        when(slackLink.getClientSecret()).thenReturn(CLIENT_SECRET);
        when(slackLink.getBotAccessToken()).thenReturn(BOT_TOKEN);
        when(slack.methods()).thenReturn(methods);
        when(slackUser.getUserToken()).thenReturn(USER_TOKEN);
        client = new DefaultSlackClient(slackHttpClient, slack, slackLink, slackUserManager, userManager,
                eventPublisher, slackResponseCache);
    }

    @Test
    public void withUser_shouldCreateClientWithCustomUser() {
        SlackUser user = mock(SlackUser.class);
        when(user.getUserToken()).thenReturn(USER_TOKEN);

        SlackClient newClient = client.withUserTokenIfAvailable(user).orElse(null);

        assertThat(newClient, notNullValue());
        assertThat(newClient.getUser().orElse(null), sameInstance(user));
    }

    @Test
    public void withUser_shouldCreateClientWithRetrievedUserByUserKey() {
        String userKey = "someUserKey";
        SlackUser newUser = mock(SlackUser.class);
        when(newUser.getUserToken()).thenReturn(USER_TOKEN);

        when(slackUserManager.getByTeamIdAndUserKey(TEAM_ID, userKey)).thenReturn(Optional.of(newUser));

        SlackClient newClient = client.withUserTokenIfAvailable(userKey).orElse(null);

        assertThat(newClient, notNullValue());
        assertThat(newClient.getUser().orElse(null), sameInstance(newUser));
    }

    @Test
    public void withRemoteUser_shouldCreateClientWithCurrentlyLoggedInUser() {
        String userKey = "someUserKey";
        UserProfile profile = mock(UserProfile.class);
        when(userManager.getRemoteUser()).thenReturn(profile);
        when(profile.getUserKey()).thenReturn(new UserKey(userKey));
        SlackUser newUser = mock(SlackUser.class);
        when(newUser.getUserToken()).thenReturn(USER_TOKEN);
        when(slackUserManager.getByTeamIdAndUserKey(TEAM_ID, userKey)).thenReturn(Optional.of(newUser));

        SlackClient newClient = client.withRemoteUserTokenIfAvailable().orElse(null);

        assertThat(newClient, notNullValue());
        assertThat(newClient.getUser().orElse(null), sameInstance(newUser));
    }

    @Test
    public void testToken_shouldCallClientWithCorrectToken() throws Exception {
        ArgumentCaptor<AuthTestRequest> captor = forClass(AuthTestRequest.class);
        AuthTestResponse responseMock = mock(AuthTestResponse.class);
        when(responseMock.isOk()).thenReturn(true);
        when(methods.authTest(captor.capture())).thenReturn(responseMock);

        Either<ErrorResponse, AuthTestResponse> response = client.testToken();

        assertThat(response.isRight(), is(true));
        assertThat(response.getOrNull(), is(responseMock));
        assertThat(captor.getValue().getToken(), is(BOT_TOKEN));
    }

    @Test
    public void getOauthAccessToken_shouldCallClientWithExpectedParameters() throws Exception {
        String code = "someCode";
        String redirectUri = "someRedirectUri";
        ArgumentCaptor<OAuthAccessRequest> captor = forClass(OAuthAccessRequest.class);
        OAuthAccessResponse responseMock = mock(OAuthAccessResponse.class);
        when(responseMock.isOk()).thenReturn(true);
        when(methods.oauthAccess(captor.capture())).thenReturn(responseMock);

        Either<ErrorResponse, OAuthAccessResponse> response = client.getOauthAccessToken(code, redirectUri);

        assertThat(response.isRight(), is(true));
        assertThat(response.getOrNull(), is(responseMock));
        OAuthAccessRequest actualRequest = captor.getValue();
        assertThat(actualRequest.getCode(), is(code));
        assertThat(actualRequest.getRedirectUri(), is(redirectUri));
        assertThat(actualRequest.getClientId(), is(CLIENT_ID));
        assertThat(actualRequest.getClientSecret(), is(CLIENT_SECRET));
    }

    @Test
    public void getAllConversations_shouldCallClientUsersConversations() throws Exception {
        ArgumentCaptor<UsersConversationsRequest> captor = forClass(UsersConversationsRequest.class);
        ResponseMetadata metadata = mock(ResponseMetadata.class);
        UsersConversationsResponse responseMock = mock(UsersConversationsResponse.class);
        when(responseMock.isOk()).thenReturn(true);
        when(responseMock.getResponseMetadata()).thenReturn(metadata);
        Conversation conversation = mock(Conversation.class);
        when(responseMock.getChannels()).thenReturn(singletonList(conversation));
        when(metadata.getNextCursor()).thenReturn("");
        when(methods.usersConversations(captor.capture())).thenReturn(responseMock);

        Either<ErrorResponse, List<Conversation>> response = client.withUserTokenIfAvailable(slackUser).get().getAllConversations();

        assertThat(response.isRight(), is(true));
        assertThat(response.getOrNull(), contains(conversation));
        UsersConversationsRequest actualRequest = captor.getValue();
        assertThat(actualRequest.getToken(), is(USER_TOKEN));
        assertThat(actualRequest.getCursor(), nullValue());
        assertThat(actualRequest.getTypes(), contains(ConversationType.PUBLIC_CHANNEL, ConversationType.PRIVATE_CHANNEL));
    }

    @Test
    public void getAllConversations_shouldCallClientListConversations() throws Exception {
        String oldValue = System.setProperty(SLACK_CLIENT_LIST_ALL_CONVERSATIONS_SYSTEM_PROPERTY, "true");
        try {
            ArgumentCaptor<ConversationsListRequest> captor = forClass(ConversationsListRequest.class);
            ResponseMetadata metadata = mock(ResponseMetadata.class);
            ConversationsListResponse responseMock = mock(ConversationsListResponse.class);
            when(responseMock.isOk()).thenReturn(true);
            when(responseMock.getResponseMetadata()).thenReturn(metadata);
            Conversation conversation = mock(Conversation.class);
            when(responseMock.getChannels()).thenReturn(singletonList(conversation));
            when(metadata.getNextCursor()).thenReturn("");
            when(methods.conversationsList(captor.capture())).thenReturn(responseMock);

            Either<ErrorResponse, List<Conversation>> response = client.withUserTokenIfAvailable(slackUser).get().getAllConversations();

            assertThat(response.isRight(), is(true));
            assertThat(response.getOrNull(), contains(conversation));
            ConversationsListRequest actualRequest = captor.getValue();
            assertThat(actualRequest.getToken(), is(USER_TOKEN));
            assertThat(actualRequest.getCursor(), nullValue());
            assertThat(actualRequest.getTypes(), contains(ConversationType.PUBLIC_CHANNEL, ConversationType.PRIVATE_CHANNEL));
        } finally {
            if (oldValue == null) {
                System.clearProperty(SLACK_CLIENT_LIST_ALL_CONVERSATIONS_SYSTEM_PROPERTY);
            } else {
                System.setProperty(SLACK_CLIENT_LIST_ALL_CONVERSATIONS_SYSTEM_PROPERTY, oldValue);
            }
        }
    }


    @Test
    public void getConversationsInfo_shouldCallClientWithExpectedParameters() throws Exception {
        String conversationId = "someConversationId";
        ArgumentCaptor<ConversationsInfoRequest> captor = forClass(ConversationsInfoRequest.class);
        ConversationsInfoResponse responseMock = mock(ConversationsInfoResponse.class);
        Conversation conversation = mock(Conversation.class);
        when(responseMock.isOk()).thenReturn(true);
        when(responseMock.getChannel()).thenReturn(conversation);
        when(methods.conversationsInfo(captor.capture())).thenReturn(responseMock);
        when(slackResponseCache.getAndCacheIfSuccessful(
                eq(BOT_TOKEN), eq(CONVERSATIONS_INFO), eq(conversationId), any(), eq(ConversationsInfoResponse.class)
        )).thenAnswer(a -> ((ResponseSupplier<?>) a.getArgument(3)).get());

        Either<ErrorResponse, Conversation> response = client.getConversationsInfo(conversationId);

        assertThat(response.isRight(), is(true));
        assertThat(response.getOrNull(), is(conversation));
        ConversationsInfoRequest actualRequest = captor.getValue();
        assertThat(actualRequest.getChannel(), is(conversationId));
        assertThat(actualRequest.getToken(), is(BOT_TOKEN));
    }

    @Test
    public void createConversation_shouldCallClientWithExpectedParameters() throws Exception {
        String name = "someConversationName";
        ArgumentCaptor<ConversationsCreateRequest> captor = forClass(ConversationsCreateRequest.class);
        ConversationsCreateResponse responseMock = mock(ConversationsCreateResponse.class);
        Conversation conversation = mock(Conversation.class);
        when(conversation.getName()).thenReturn(name);
        when(responseMock.isOk()).thenReturn(true);
        when(responseMock.getChannel()).thenReturn(conversation);
        when(methods.conversationsCreate(captor.capture())).thenReturn(responseMock);

        Either<ErrorResponse, Conversation> response = client.withUserTokenIfAvailable(slackUser).get().createConversation(name);

        assertThat(response.isRight(), is(true));
        assertThat(response.getOrNull(), is(conversation));
        ConversationsCreateRequest actualRequest = captor.getValue();
        assertThat(actualRequest.getName(), is(name));
        assertThat(actualRequest.getToken(), is(USER_TOKEN));
        assertThat(actualRequest.isPrivate(), is(false));
    }

    @Test
    public void setConversationTopic_shouldCallClientWithExpectedParameters() throws Exception {
        String conversationId = "someConversationId";
        String topic = "someConversationTopic";
        ArgumentCaptor<ConversationsSetTopicRequest> captor = forClass(ConversationsSetTopicRequest.class);
        ConversationsSetTopicResponse responseMock = mock(ConversationsSetTopicResponse.class);
        Conversation conversation = mock(Conversation.class);
        when(responseMock.isOk()).thenReturn(true);
        when(responseMock.getChannel()).thenReturn(conversation);
        when(methods.conversationsSetTopic(captor.capture())).thenReturn(responseMock);

        Either<ErrorResponse, Conversation> response = client.withUserTokenIfAvailable(slackUser).get().setConversationTopic(conversationId, topic);

        assertThat(response.isRight(), is(true));
        assertThat(response.getOrNull(), is(conversation));
        ConversationsSetTopicRequest actualRequest = captor.getValue();
        assertThat(actualRequest.getToken(), is(USER_TOKEN));
        assertThat(actualRequest.getChannel(), is(conversationId));
        assertThat(actualRequest.getTopic(), is(topic));
    }

    @Test
    public void selfInviteToConversation_shouldCallClientWithExpectedParameters() throws Exception {
        String conversationId = "someConversationId";
        String botUserId = "someBotId";
        ArgumentCaptor<ConversationsInviteRequest> captor = forClass(ConversationsInviteRequest.class);
        ConversationsInviteResponse responseMock = mock(ConversationsInviteResponse.class);
        Conversation conversation = mock(Conversation.class);
        when(responseMock.isOk()).thenReturn(true);
        when(responseMock.getChannel()).thenReturn(conversation);
        when(methods.conversationsInvite(captor.capture())).thenReturn(responseMock);
        when(slackLink.getBotUserId()).thenReturn(botUserId);

        Either<ErrorResponse, Conversation> response = client.withUserTokenIfAvailable(slackUser).get().selfInviteToConversation(conversationId);

        assertThat(response.isRight(), is(true));
        assertThat(response.getOrNull(), is(conversation));
        ConversationsInviteRequest actualRequest = captor.getValue();
        assertThat(actualRequest.getToken(), is(USER_TOKEN));
        assertThat(actualRequest.getChannel(), is(conversationId));
        assertThat(actualRequest.getUsers(), contains(botUserId));
    }

    @Test
    public void getUserInfo_shouldCallClientWithExpectedParameters() throws Exception {
        String userId = "someConversationId";
        ArgumentCaptor<UsersInfoRequest> captor = forClass(UsersInfoRequest.class);
        UsersInfoResponse responseMock = mock(UsersInfoResponse.class);
        User user = mock(User.class);
        when(responseMock.isOk()).thenReturn(true);
        when(responseMock.getUser()).thenReturn(user);
        when(methods.usersInfo(captor.capture())).thenReturn(responseMock);
        when(slackResponseCache.getAndCacheIfSuccessful(
                eq("bot"), eq(USERS_INFO), eq(userId), any(), eq(UsersInfoResponse.class)
        )).thenAnswer(a -> ((ResponseSupplier<?>) a.getArgument(3)).get());

        Either<ErrorResponse, User> response = client.getUserInfo(userId);

        assertThat(response.isRight(), is(true));
        assertThat(response.getOrNull(), is(user));
        UsersInfoRequest actualRequest = captor.getValue();
        assertThat(actualRequest.getUser(), is(userId));
        assertThat(actualRequest.getToken(), is(BOT_TOKEN));
    }

    @Test
    public void postMessage_shouldCallClientWithExpectedParameters() throws Exception {
        ChatPostMessageRequest request = ChatPostMessageRequest.builder()
                .attachments(emptyList())
                .channel("someChannelId")
                .parse("someParse")
                .text("someText")
                .asUser(true)
                .iconEmoji("someEmoji")
                .iconUrl("someIconUrl")
                .linkNames(true)
                .mrkdwn(true)
                .replyBroadcast(true)
                .threadTs("someThreadTs")
                .unfurlLinks(true)
                .unfurlMedia(true)
                .username("someUserName")
                .build();
        ChatPostMessageResponse responseMock = mock(ChatPostMessageResponse.class);
        Message message = mock(Message.class);
        when(responseMock.isOk()).thenReturn(true);
        when(responseMock.getMessage()).thenReturn(message);
        ArgumentCaptor<ChatPostMessageRequest> captor = forClass(ChatPostMessageRequest.class);
        when(methods.chatPostMessage(captor.capture())).thenReturn(responseMock);

        Either<ErrorResponse, Message> response = client.postMessage(request);

        assertThat(response.isRight(), is(true));
        assertThat(response.getOrNull(), is(message));
        ChatPostMessageRequest actualRequest = captor.getValue();
        assertThat(actualRequest.getChannel(), is(request.getChannel()));
        assertThat(actualRequest.getToken(), is(BOT_TOKEN));
        assertThat(actualRequest.getParse(), is(request.getParse()));
        assertThat(actualRequest.getText(), is(request.getText()));
        assertThat(actualRequest.isAsUser(), is(request.isAsUser()));
        assertThat(actualRequest.getIconEmoji(), is(request.getIconEmoji()));
        assertThat(actualRequest.getIconUrl(), is(request.getIconUrl()));
        assertThat(actualRequest.isLinkNames(), is(request.isLinkNames()));
        assertThat(actualRequest.isMrkdwn(), is(request.isMrkdwn()));
        assertThat(actualRequest.isReplyBroadcast(), is(request.isReplyBroadcast()));
        assertThat(actualRequest.getThreadTs(), is(request.getThreadTs()));
        assertThat(actualRequest.isUnfurlLinks(), is(request.isUnfurlLinks()));
        assertThat(actualRequest.isUnfurlMedia(), is(request.isUnfurlMedia()));
        assertThat(actualRequest.getUsername(), is(request.getUsername()));
    }

    @Test
    public void postDirectMessage_shouldCallClientWithExpectedParameters() throws Exception {
        String slackUserId = "someSlackUserId";
        String conversationId = "someConversationId";
        ChatPostMessageRequest request = ChatPostMessageRequest.builder()
                .attachments(emptyList())
                .parse("someParse")
                .text("someText")
                .asUser(true)
                .iconEmoji("someEmoji")
                .iconUrl("someIconUrl")
                .linkNames(true)
                .mrkdwn(true)
                .replyBroadcast(true)
                .threadTs("someThreadTs")
                .unfurlLinks(true)
                .unfurlMedia(true)
                .username("someUserName")
                .build();

        ConversationsOpenResponse initialResponseMock = mock(ConversationsOpenResponse.class);
        when(initialResponseMock.isOk()).thenReturn(true);
        Conversation conversation = mock(Conversation.class);
        when(conversation.getId()).thenReturn(conversationId);
        when(initialResponseMock.getChannel()).thenReturn(conversation);
        ArgumentCaptor<ConversationsOpenRequest> initialCaptor = forClass(ConversationsOpenRequest.class);
        when(slackResponseCache.getAndCacheIfSuccessful(
                eq("bot"), eq(CONVERSATIONS_OPEN), eq(slackUserId), any(), eq(ConversationsOpenResponse.class)
        )).thenAnswer(a -> ((ResponseSupplier<?>) a.getArgument(3)).get());
        when(methods.conversationsOpen(initialCaptor.capture())).thenReturn(initialResponseMock);

        ChatPostMessageResponse responseMock = mock(ChatPostMessageResponse.class);
        Message message = mock(Message.class);
        when(responseMock.isOk()).thenReturn(true);
        when(responseMock.getMessage()).thenReturn(message);
        ArgumentCaptor<ChatPostMessageRequest> captor = forClass(ChatPostMessageRequest.class);
        when(methods.chatPostMessage(captor.capture())).thenReturn(responseMock);

        Either<ErrorResponse, Message> response = client.postDirectMessage(slackUserId, request);

        assertThat(response.isRight(), is(true));
        assertThat(response.getOrNull(), is(message));

        ConversationsOpenRequest initialActualRequest = initialCaptor.getValue();
        assertThat(initialActualRequest.getUsers(), contains(slackUserId));
        assertThat(initialActualRequest.getToken(), is(BOT_TOKEN));

        ChatPostMessageRequest actualRequest = captor.getValue();
        assertThat(actualRequest.getChannel(), is(conversationId));
        assertThat(actualRequest.getToken(), is(BOT_TOKEN));
        assertThat(actualRequest.getParse(), is(request.getParse()));
        assertThat(actualRequest.getText(), is(request.getText()));
        assertThat(actualRequest.isAsUser(), is(request.isAsUser()));
        assertThat(actualRequest.getIconEmoji(), is(request.getIconEmoji()));
        assertThat(actualRequest.getIconUrl(), is(request.getIconUrl()));
        assertThat(actualRequest.isLinkNames(), is(request.isLinkNames()));
        assertThat(actualRequest.isMrkdwn(), is(request.isMrkdwn()));
        assertThat(actualRequest.isReplyBroadcast(), is(request.isReplyBroadcast()));
        assertThat(actualRequest.getThreadTs(), is(request.getThreadTs()));
        assertThat(actualRequest.isUnfurlLinks(), is(request.isUnfurlLinks()));
        assertThat(actualRequest.isUnfurlMedia(), is(request.isUnfurlMedia()));
        assertThat(actualRequest.getUsername(), is(request.getUsername()));
    }

    @Test
    public void postResponse_shouldCallClientWithExpectedParameters() throws Exception {
        String url = "url";
        String type = "type";
        ChatPostMessageRequest request = ChatPostMessageRequest.builder()
                .text("someText")
                .attachments(new ArrayList<>())
                .blocks(singletonList(SectionBlock.builder().build()))
                .build();
        ArgumentCaptor<DelayedSlackMessage> captor = forClass(DelayedSlackMessage.class);
        Response responseStub = new Response.Builder()
                .request(new Request.Builder().url("https://some.com").build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("message")
                .build();
        when(slackHttpClient.postJsonPostRequest(eq(url), captor.capture())).thenReturn(responseStub);

        Either<ErrorResponse, Boolean> response = client.postResponse(url, type, request);

        assertThat(response.isRight(), is(true));
        assertThat(response.getOrNull(), is(true));
        DelayedSlackMessage message = captor.getValue();
        assertThat(message.getText(), is(request.getText()));
        assertThat(message.getAttachments(), is(request.getAttachments()));
        assertThat(message.getBlocks(), is(request.getBlocks()));
        assertThat(message.getResponseType(), is(type));
    }

    @Test
    public void unfurl_shouldCallClientWithExpectedParameters() throws Exception {
        String conversationId = "someConversationId";
        String messageTimestamp = "messageTimestamp";
        String permalink = "someLink";
        Attachment attachment = Attachment.builder().text("someMessage").build();
        ArgumentCaptor<ChatUnfurlRequest> captor = forClass(ChatUnfurlRequest.class);
        ChatUnfurlResponse responseMock = mock(ChatUnfurlResponse.class);
        when(responseMock.isOk()).thenReturn(true);
        when(methods.chatUnfurl(captor.capture())).thenReturn(responseMock);

        Map<String, Attachment> attachmentMap = ImmutableMap.of(permalink, attachment);

        Either<ErrorResponse, Boolean> response = client.withUserTokenIfAvailable(slackUser).get()
                .unfurl(conversationId, messageTimestamp, attachmentMap);

        assertThat(response.isRight(), is(true));
        assertThat(response.getOrNull(), is(true));
        ChatUnfurlRequest actualRequest = captor.getValue();
        assertThat(actualRequest.getChannel(), is(conversationId));
        assertThat(actualRequest.getTs(), is(messageTimestamp));
        assertThat(actualRequest.getToken(), is(USER_TOKEN));
        assertThat(actualRequest.getUnfurls(), is(GsonFactory.createSnakeCase().toJson(attachmentMap)));
    }

    @Test
    public void getPermalink_shouldCallClientWithExpectedParameters() throws Exception {
        String conversationId = "someConversationId";
        String messageTimestamp = "messageTimestamp";
        String permalink = "someLink";
        ArgumentCaptor<ChatGetPermalinkRequest> captor = forClass(ChatGetPermalinkRequest.class);
        ChatGetPermalinkResponse responseMock = mock(ChatGetPermalinkResponse.class);
        when(responseMock.isOk()).thenReturn(true);
        when(responseMock.getPermalink()).thenReturn(permalink);
        when(methods.chatGetPermalink(captor.capture())).thenReturn(responseMock);

        Either<ErrorResponse, String> response = client.getPermalink(conversationId, messageTimestamp);

        assertThat(response.isRight(), is(true));
        assertThat(response.getOrNull(), is(permalink));
        ChatGetPermalinkRequest actualRequest = captor.getValue();
        assertThat(actualRequest.getChannel(), is(conversationId));
        assertThat(actualRequest.getMessageTs(), is(messageTimestamp));
        assertThat(actualRequest.getToken(), is(BOT_TOKEN));
    }

    @Test
    public void openDialog_shouldCallClientWithExpectedParameters() throws Exception {
        String triggerId = "someTriggerId";
        Dialog dialog = mock(Dialog.class);
        ArgumentCaptor<DialogOpenRequest> captor = forClass(DialogOpenRequest.class);
        DialogOpenResponse responseMock = mock(DialogOpenResponse.class);
        when(responseMock.isOk()).thenReturn(true);
        when(methods.dialogOpen(captor.capture())).thenReturn(responseMock);

        Either<ErrorResponse, Boolean> response = client.dialogOpen(triggerId, dialog);

        assertThat(response.isRight(), is(true));
        DialogOpenRequest actualRequest = captor.getValue();
        assertThat(actualRequest.getTriggerId(), is(triggerId));
        assertThat(actualRequest.getDialog(), is(dialog));
        assertThat(actualRequest.getToken(), is(BOT_TOKEN));
    }
}
