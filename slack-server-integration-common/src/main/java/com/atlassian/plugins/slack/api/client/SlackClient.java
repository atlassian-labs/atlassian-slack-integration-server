package com.atlassian.plugins.slack.api.client;

import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.SlackUser;
import com.atlassian.plugins.slack.api.UserNotLinkedException;
import com.atlassian.plugins.slack.util.ErrorResponse;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostEphemeralRequest;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import com.github.seratch.jslack.api.methods.response.auth.AuthTestResponse;
import com.github.seratch.jslack.api.methods.response.oauth.OAuthAccessResponse;
import com.github.seratch.jslack.api.model.Attachment;
import com.github.seratch.jslack.api.model.Conversation;
import com.github.seratch.jslack.api.model.Message;
import com.github.seratch.jslack.api.model.User;
import com.github.seratch.jslack.api.model.dialog.Dialog;
import io.atlassian.fugue.Either;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface SlackClient {

    /**
     * It returns a new client that uses the user token, if available.
     */
    Optional<SlackClient> withUserTokenIfAvailable(SlackUser slackUser);

    /**
     * It returns a new client that uses the user token, if available.
     */
    Optional<SlackClient> withUserTokenIfAvailable(String userKey);

    Either<UserNotLinkedException, SlackClient> withUserToken(String userKey);

    /**
     * It returns a new client that uses the current logged-in user token, if available.
     */
    Optional<SlackClient> withRemoteUserTokenIfAvailable();

    Either<UserNotLinkedException, SlackClient> withRemoteUser();

    /**
     * It returns a new client that uses the App installer user token.
     */
    SlackClient withInstallerUserToken();

    SlackLink getLink();

    @Nonnull
    Optional<SlackUser> getUser();

    // token

    Either<ErrorResponse, AuthTestResponse> testToken();

    Either<ErrorResponse, OAuthAccessResponse> getOauthAccessToken(String code, String redirectUri);

    // conversations

    Either<ErrorResponse, List<Conversation>> getAllConversations();

    Either<ErrorResponse, Conversation> getConversationsInfo(String conversationId);

    Either<ErrorResponse, Conversation> createConversation(String name);

    Either<ErrorResponse, Conversation> setConversationTopic(String conversationId, String topic);

    Either<ErrorResponse, Conversation> selfInviteToConversation(String conversationId);

    // users

    Either<ErrorResponse, User> getUserInfo(String slackUserId);

    // messages

    Either<ErrorResponse, Message> postMessage(ChatPostMessageRequest messageRequest);

    Either<ErrorResponse, String> postEphemeralMessage(ChatPostEphemeralRequest messageRequest);

    Either<ErrorResponse, Boolean> postResponse(String responseUrl,
                                                String responseType,
                                                ChatPostMessageRequest messageRequest);

    Either<ErrorResponse, Message> postDirectMessage(String slackUserId, ChatPostMessageRequest messageRequest);

    Either<ErrorResponse, Boolean> unfurl(String conversationId,
                                          String messageTimestamp,
                                          Map<String, Attachment> unfurls);

    Either<ErrorResponse, Boolean> unfurlWithoutAuthentication(String conversationId,
                                                               String messageTimestamp,
                                                               String oAuthErrorMessage);

    Either<ErrorResponse, String> getPermalink(String conversationId, String messageTimestamp);

    Either<ErrorResponse, Boolean> dialogOpen(String triggerId, Dialog dialog);

}
