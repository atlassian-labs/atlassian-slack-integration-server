package com.atlassian.bitbucket.plugins.slack.listener;

import com.atlassian.bitbucket.comment.AddCommentReplyRequest;
import com.atlassian.bitbucket.comment.CommentService;
import com.atlassian.bitbucket.user.ApplicationUser;
import com.atlassian.bitbucket.user.EscalatedSecurityContext;
import com.atlassian.bitbucket.user.SecurityService;
import com.atlassian.bitbucket.user.UserService;
import com.atlassian.bitbucket.util.Operation;
import com.atlassian.plugins.slack.api.SlackUser;
import com.atlassian.plugins.slack.api.client.SlackClient;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.atlassian.plugins.slack.api.webhooks.action.BlockKitAction;
import com.atlassian.plugins.slack.api.webhooks.action.BlockSlackAction;
import com.atlassian.plugins.slack.api.webhooks.action.DialogSubmissionSlackAction;
import com.atlassian.plugins.slack.user.SlackUserManager;
import com.atlassian.plugins.slack.util.AsyncExecutor;
import com.atlassian.sal.api.message.I18nResolver;
import com.github.seratch.jslack.api.model.dialog.Dialog;
import com.github.seratch.jslack.api.model.dialog.DialogElement;
import com.google.common.collect.ImmutableMap;
import io.atlassian.fugue.Either;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static com.atlassian.plugins.slack.test.util.CommonTestUtil.bypass;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.AdditionalAnswers.answer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SlackActionEventListenerTest {
    public static final String TEAM_ID = "someTeamId";
    public static final String TRIGGER_ID = "someTriggerId";

    @Mock
    CommentService commentService;
    @Mock
    SecurityService securityService;
    @Mock
    UserService userService;
    @Mock
    I18nResolver i18nResolver;
    @Mock
    SlackClientProvider slackClientProvider;
    @Mock
    SlackClient slackClient;
    @Mock
    SlackUserManager slackUserManager;
    @Mock
    AsyncExecutor asyncExecutor;

    @Mock
    BlockSlackAction buttonAction;
    @Mock
    DialogSubmissionSlackAction dialogAction;
    @Mock
    BlockKitAction replyButton;
    @Mock
    SlackUser slackUser;
    @Mock
    ApplicationUser applicationUser;
    @Mock
    EscalatedSecurityContext securityContext;
    @Captor
    ArgumentCaptor<Dialog> dialogCaptor;
    @Captor
    ArgumentCaptor<AddCommentReplyRequest> replyCaptor;
    @InjectMocks
    SlackActionEventListener listener;

    @BeforeEach
    void beforeEach() {
        bypass(asyncExecutor);
        when(i18nResolver.getText(anyString())).then(answer(key -> key));
    }

    @Test
    void onCommentReply_shouldCallOpenDialog_onEvent() {
        when(slackClientProvider.withTeamId(TEAM_ID)).thenReturn(Either.right(slackClient));
        when(slackClient.dialogOpen(eq(TRIGGER_ID), dialogCaptor.capture())).thenReturn(Either.right(true));
        when(buttonAction.getTeamId()).thenReturn(TEAM_ID);
        when(buttonAction.getTriggerId()).thenReturn(TRIGGER_ID);
        when(buttonAction.getActions()).thenReturn(singletonList(replyButton));
        String callbackId = "someCallbackId";
        when(replyButton.getValue()).thenReturn(callbackId);

        listener.onCommentReplyAction(buttonAction);

        Dialog dialog = dialogCaptor.getValue();
        assertEquals(dialog.getCallbackId(), callbackId);
        assertEquals(dialog.getElements().size(), 1);
        DialogElement field = dialog.getElements().get(0);
        assertEquals(field.getType(), "textarea");
        assertEquals(field.getName(), SlackActionEventListener.COMMENT_FIELD_NAME);
    }

    @SuppressWarnings("RedundantThrows")
    @Test
    void onDialogSubmission_shouldCreateComment_onEvent() throws Throwable {
        String comment = "someComment";
        when(dialogAction.getSubmission()).thenReturn(ImmutableMap.of(SlackActionEventListener.COMMENT_FIELD_NAME, comment));
        when(dialogAction.getCallbackId()).thenReturn("{\"commentId\": 12345}");
        String slackUserId = "someUserId";
        when(dialogAction.getUserId()).thenReturn(slackUserId);
        when(slackUserManager.getBySlackUserId(slackUserId)).thenReturn(Optional.of(slackUser));
        when(slackUser.getUserKey()).thenReturn("7");
        when(userService.getUserById(7)).thenReturn(applicationUser);
        when(securityService.impersonating(eq(applicationUser), anyString())).thenReturn(securityContext);
        when(securityContext.call(any())).thenAnswer(answer((Operation op) -> op.perform()));

        listener.onDialogSubmission(dialogAction);

        verify(commentService).addReply(replyCaptor.capture());
        AddCommentReplyRequest request = replyCaptor.getValue();
        assertThat(request.getParentId(), equalTo(12345L));
        assertThat(request.getText(), equalTo(comment));
    }
}
