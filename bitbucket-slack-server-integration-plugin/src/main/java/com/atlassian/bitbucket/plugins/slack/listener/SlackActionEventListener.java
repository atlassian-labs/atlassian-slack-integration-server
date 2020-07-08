package com.atlassian.bitbucket.plugins.slack.listener;

import com.atlassian.bitbucket.comment.AddCommentReplyRequest;
import com.atlassian.bitbucket.comment.CommentService;
import com.atlassian.bitbucket.plugins.slack.model.ReplyToCommentPayload;
import com.atlassian.bitbucket.user.ApplicationUser;
import com.atlassian.bitbucket.user.SecurityService;
import com.atlassian.bitbucket.user.UserService;
import com.atlassian.event.api.EventListener;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.atlassian.plugins.slack.api.webhooks.action.BlockSlackAction;
import com.atlassian.plugins.slack.api.webhooks.action.DialogSubmissionSlackAction;
import com.atlassian.plugins.slack.user.SlackUserManager;
import com.atlassian.plugins.slack.util.AsyncExecutor;
import com.atlassian.plugins.slack.util.ErrorResponse;
import com.atlassian.sal.api.message.I18nResolver;
import com.github.seratch.jslack.api.model.dialog.Dialog;
import com.github.seratch.jslack.api.model.dialog.DialogElement;
import com.github.seratch.jslack.api.model.dialog.DialogTextAreaElement;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static java.util.Collections.singletonList;

@Component
public class SlackActionEventListener {
    public static final String COMMENT_FIELD_NAME = "comment";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(SlackActionEventListener.class);

    private final CommentService commentService;
    private final SecurityService securityService;
    private final UserService userService;
    private final I18nResolver i18nResolver;
    private final SlackClientProvider slackClientProvider;
    private final SlackUserManager slackUserManager;
    private final AsyncExecutor asyncExecutor;

    @Autowired
    public SlackActionEventListener(final CommentService commentService,
                                    final SecurityService securityService,
                                    final UserService userService,
                                    final I18nResolver i18nResolver,
                                    final SlackClientProvider slackClientProvider,
                                    final SlackUserManager slackUserManager,
                                    final AsyncExecutor asyncExecutor) {
        this.commentService = commentService;
        this.securityService = securityService;
        this.userService = userService;
        this.i18nResolver = i18nResolver;
        this.slackClientProvider = slackClientProvider;
        this.slackUserManager = slackUserManager;
        this.asyncExecutor = asyncExecutor;
    }

    @EventListener
    public void onCommentReplyAction(final BlockSlackAction action) {
        log.debug("Got interactive message action {}", action);

        asyncExecutor.run(() -> slackClientProvider.withTeamId(action.getTeamId())
                .leftMap(ErrorResponse::new)
                .flatMap(client -> client.dialogOpen(action.getTriggerId(), replyToCommentDialog(action)))
                .left()
                .forEach(response -> {
                    log.error("Failed to open a dialog: {}", response.getMessage());
                }));
    }

    private Dialog replyToCommentDialog(final BlockSlackAction action) {
        final String itemDescriptor = action.getActions().get(0).getValue();
        final DialogElement commentField = DialogTextAreaElement.builder()
                .name(COMMENT_FIELD_NAME)
                .label(i18nResolver.getText("slack.dialog.reply.comment.field.title"))
                .maxLength(3000)
                .build();
        return Dialog.builder()
                .title(i18nResolver.getText("slack.dialog.reply.comment.title"))
                .submitLabel(i18nResolver.getText("slack.dialog.reply.comment.submit"))
                .callbackId(itemDescriptor)
                .elements(singletonList(commentField))
                .build();
    }

    @EventListener
    public void onDialogSubmission(final DialogSubmissionSlackAction action) {
        log.debug("Got dialog submission action {}", action);

        asyncExecutor.run(() -> {
            final String comment = (String) action.getSubmission().get(COMMENT_FIELD_NAME);
            final String callbackId = action.getCallbackId();
            try {
                final ReplyToCommentPayload replyToCommentPayload = OBJECT_MAPPER.readValue(callbackId, ReplyToCommentPayload.class);
                slackUserManager.getBySlackUserId(action.getUserId())
                        .ifPresent(slackUser -> {
                            final ApplicationUser user = userService.getUserById(Integer.valueOf(slackUser.getUserKey()));
                            final AddCommentReplyRequest request = new AddCommentReplyRequest.Builder(
                                    replyToCommentPayload.getCommentId(), comment).build();
                            securityService.impersonating(user, "Slack integration posts a comment")
                                    .call(() -> commentService.addReply(request));
                        });
            } catch (IOException e) {
                log.error("Failed to parse callback id", e);
            }
        });
    }
}
