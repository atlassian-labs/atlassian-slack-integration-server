package com.atlassian.bitbucket.plugins.slack.notification.renderer;

import com.atlassian.bitbucket.comment.Comment;
import com.atlassian.bitbucket.comment.CommentAction;
import com.atlassian.bitbucket.commit.Commit;
import com.atlassian.bitbucket.commit.CommitRequest;
import com.atlassian.bitbucket.commit.CommitService;
import com.atlassian.bitbucket.commit.CommitsBetweenRequest;
import com.atlassian.bitbucket.event.commit.CommitDiscussionCommentEvent;
import com.atlassian.bitbucket.event.pull.PullRequestCommentEvent;
import com.atlassian.bitbucket.event.pull.PullRequestEvent;
import com.atlassian.bitbucket.event.pull.PullRequestOpenedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestParticipantUnapprovedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestReviewersUpdatedEvent;
import com.atlassian.bitbucket.event.repository.RepositoryForkedEvent;
import com.atlassian.bitbucket.event.repository.RepositoryRefsChangedEvent;
import com.atlassian.bitbucket.event.task.TaskEvent;
import com.atlassian.bitbucket.permission.Permission;
import com.atlassian.bitbucket.plugins.slack.event.RepositoryLinkedEvent;
import com.atlassian.bitbucket.plugins.slack.model.ReplyToCommentPayload;
import com.atlassian.bitbucket.plugins.slack.notification.TaskNotificationTypes;
import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.bitbucket.pull.PullRequestParticipantStatus;
import com.atlassian.bitbucket.pull.PullRequestRef;
import com.atlassian.bitbucket.repository.Ref;
import com.atlassian.bitbucket.repository.RefChange;
import com.atlassian.bitbucket.repository.RefChangeType;
import com.atlassian.bitbucket.repository.RefService;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.repository.ResolveRefRequest;
import com.atlassian.bitbucket.repository.StandardRefType;
import com.atlassian.bitbucket.server.ApplicationPropertiesService;
import com.atlassian.bitbucket.user.ApplicationUser;
import com.atlassian.bitbucket.user.SecurityService;
import com.atlassian.bitbucket.util.Page;
import com.atlassian.bitbucket.util.PageImpl;
import com.atlassian.bitbucket.util.PageRequest;
import com.atlassian.bitbucket.util.PageUtils;
import com.atlassian.plugins.slack.api.notification.Verbosity;
import com.atlassian.plugins.slack.settings.SlackSettingService;
import com.atlassian.sal.api.message.I18nResolver;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest.ChatPostMessageRequestBuilder;
import com.github.seratch.jslack.api.model.block.ActionsBlock;
import com.github.seratch.jslack.api.model.block.LayoutBlock;
import com.github.seratch.jslack.api.model.block.SectionBlock;
import com.github.seratch.jslack.api.model.block.composition.MarkdownTextObject;
import com.github.seratch.jslack.api.model.block.composition.PlainTextObject;
import com.github.seratch.jslack.api.model.block.element.BlockElement;
import com.github.seratch.jslack.api.model.block.element.ButtonElement;
import org.apache.commons.lang3.ObjectUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.atlassian.plugins.slack.util.SlackHelper.escapeSignsForSlackLink;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.defaultString;

/**
 * Generates notifications in Slack format.
 */
@Component
public class SlackNotificationRenderer {
    protected static final int MAX_COMMITS_TO_SHOW = 7;

    private static final Logger logger = LoggerFactory.getLogger(SlackNotificationRenderer.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final CommitService commitService;
    private final SecurityService securityService;
    private final RefService refService;
    private final I18nResolver i18nResolver;
    private final SlackSettingService slackSettingService;
    private final SlackLinkRenderer slackLinkRenderer;
    private final ApplicationPropertiesService applicationPropertiesService;

    @Autowired
    public SlackNotificationRenderer(
            final CommitService commitService,
            final SecurityService securityService,
            final RefService refService,
            final I18nResolver i18nResolver,
            final SlackSettingService slackSettingService,
            final SlackLinkRenderer slackLinkRenderer,
            final ApplicationPropertiesService applicationPropertiesService) {
        this.commitService = commitService;
        this.securityService = securityService;
        this.refService = refService;
        this.i18nResolver = i18nResolver;
        this.slackSettingService = slackSettingService;
        this.slackLinkRenderer = slackLinkRenderer;
        this.applicationPropertiesService = applicationPropertiesService;
    }

    private String getSiteTitle() {
        return defaultString(applicationPropertiesService.getDisplayName(), "Bitbucket");
    }

    public String getHelpMessage(final String botUserId, final String commandName) {
        final String botMention = "<@" + botUserId + ">";
        final String handleText = defaultString(commandName, botMention);
        return i18nResolver.getText(
                "slack.command.show.help",
                botMention,
                slackLinkRenderer.homePage() + "|" + escapeSignsForSlackLink(getSiteTitle()),
                handleText);
    }

    public String getAccountMessage(final ApplicationUser user) {
        if (user != null) {
            return i18nResolver.getText(
                    "slack.user.link.account.details.message",
                    slackLinkRenderer.userLink(user),
                    slackLinkRenderer.oAuthSessionsPageUrl());
        }
        return i18nResolver.getText("slack.user.link.no.account.message", slackLinkRenderer.oAuthSessionsPageUrl());
    }

    public String getInvalidCommandMessage() {
        return i18nResolver.getText("slack.command.invalid");
    }

    public String getPleaseAuthenticateMessage() {
        return i18nResolver.getText("slack.user.link.confirm.account.message", slackLinkRenderer.oAuthSessionsPageUrl());
    }

    public String getWelcomeMessage(final String teamId) {
        return i18nResolver.getText(
                "slack.notification.workspace.connected.welcome",
                slackLinkRenderer.homePage() + "|" + escapeSignsForSlackLink(getSiteTitle()),
                slackLinkRenderer.adminConfigurationPage(teamId));
    }

    public ChatPostMessageRequestBuilder getRepositoryLinkedMessage(final RepositoryLinkedEvent event) {
        ApplicationUser applicationUser = event.getUser();
        String userProfileLink = slackLinkRenderer.userLink(applicationUser);

        final String text = i18nResolver.getText("slack.notification.channel-linked",
                userProfileLink,
                slackLinkRenderer.repoLink(event.getRepository()));
        return standardBlockMessage(text)
                .channel(event.getChannelId());
    }

    public ChatPostMessageRequestBuilder getRepositoryForkedMessage(final RepositoryForkedEvent event) {
        ApplicationUser user = event.getUser();
        Repository repository = event.getRepository();

        final String text = i18nResolver.getText("slack.activity.repository.forked",
                slackLinkRenderer.userLink(user),
                slackLinkRenderer.repoLink(repository.getOrigin()),
                slackLinkRenderer.repoLink(repository));
        return standardBlockMessage(text);
    }

    public ChatPostMessageRequestBuilder getPushMessage(final RepositoryRefsChangedEvent event,
                                                        final RefChange refChange,
                                                        final Verbosity verbosity) {
        final Repository repository = event.getRepository();
        final String userLink = slackLinkRenderer.userLink(event.getUser());
        final String repoLink = slackLinkRenderer.repoLink(repository);
        final Ref ref = refChange.getType() == RefChangeType.DELETE
                ? null
                : refService.resolveRef(new ResolveRefRequest.Builder(repository)
                .refId(refChange.getRef().getId())
                .build());
        final String refLink = ref == null
                ? "`" + refChange.getRef().getDisplayId() + "`"
                : slackLinkRenderer.refLink(repository, ref);

        // tag
        if (refChange.getRef().getType() == StandardRefType.TAG) {
            final String text = i18nResolver.getText(
                    "slack.activity.commit.tag." + refChange.getType().name().toLowerCase(),
                    userLink,
                    refLink,
                    repoLink);
            return standardBlockMessage(text);
        }

        // try to find commits for branch
        final Page<Commit> commitsPage = getAddedCommitsToRender(refChange, repository);
        final List<Commit> commits = newArrayList(commitsPage.getValues());
        final int commitsCount = commits.size();

        if (commitsCount == 0) {
            final String text = i18nResolver.getText(
                    "slack.activity.commit.branch." + refChange.getType().name().toLowerCase(),
                    userLink,
                    refLink,
                    repoLink);
            return standardBlockMessage(text);
        }

        final String commitsCountLabel = commitsPage.getIsLastPage() ? String.valueOf(commitsCount) : commitsCount + "+";
        final String text = commitsCount == 1
                ? i18nResolver.getText("slack.activity.commit.pushed.one", userLink, refLink, repoLink)
                : i18nResolver.getText("slack.activity.commit.pushed.multiple", userLink, commitsCountLabel,
                refLink, repoLink);
        final String fullMessage;
        if (verbosity == Verbosity.BASIC) {
            fullMessage = text;
        } else {
            final String formattedCommits = slackLinkRenderer.formatCommitList(commits);
            fullMessage = text + "\n" + slackLinkRenderer.slackMultilineQuote(formattedCommits);
        }
        return standardBlockMessage(text, fullMessage);
    }

    private Page<Commit> getAddedCommitsToRender(final RefChange refChange, final Repository repository) {
        RefChangeType refChangeType = refChange.getType();
        PageRequest pageRequest = PageUtils.newRequest(0, MAX_COMMITS_TO_SHOW);
        if (refChangeType == RefChangeType.DELETE) {
            // For deleted refs, don't try to retrieve any commits. Since we're only streaming "added" commits,
            // there aren't any if the ref was deleted
            return new PageImpl<>(pageRequest, 0, emptyList(), true);
        }

        CommitsBetweenRequest.Builder builder = new CommitsBetweenRequest.Builder(repository)
                .include(refChange.getToHash());
        // If the ref was updated, exclude the previous tip. This way only "added" commits will stream
        if (refChangeType == RefChangeType.UPDATE) {
            builder.exclude(refChange.getFromHash());
        }
        Page<Commit> commitsPage = commitService.getCommitsBetween(builder.build(), pageRequest);

        return commitsPage;
    }

    public ChatPostMessageRequestBuilder getCommitDiscussionMessage(final CommitDiscussionCommentEvent event) {
        Repository repository = event.getRepository();
        String commitId = event.getDiscussion().getCommitId();
        Comment comment = event.getComment();
        ApplicationUser author = comment.getAuthor();
        Commit commit = findCommit(repository, commitId);
        CommentAction commentAction = event.getCommentAction();
        String commentText = comment.getText();

        String title = i18nResolver.getText(
                "slack.activity.commit.comment." + commentAction.name().toLowerCase(),
                slackLinkRenderer.userLink(author),
                slackLinkRenderer.commitLink(commit),
                slackLinkRenderer.repoLink(repository));
        List<BlockElement> buttons = new ArrayList<>();

        // for deletion, do not send details
        if (commentAction == CommentAction.DELETED) {
            return standardBlockMessage(
                    title + ": " + commentText,
                    title + "\n" + slackLinkRenderer.slackMultilineQuote(commentText));
        }

        // show interactive 'Reply' button only if Slack is able to deliver click action to BBS
        if (slackSettingService.isInstancePublic()) {
            String replyButtonValue = buildReplyToCommentButtonValue(ReplyToCommentPayload.TYPE_COMMIT,
                    commit.getId(), repository.getId(), comment.getId());
            ButtonElement replyButton = ButtonElement.builder()
                    .text(PlainTextObject.builder()
                            .text(i18nResolver.getText("slack.activity.commit.action.reply"))
                            .build())
                    .value(replyButtonValue)
                    .build();
            buttons.add(replyButton);
        }
        BlockElement viewCommitCommentButton = ButtonElement.builder()
                .text(PlainTextObject.builder()
                        .text(i18nResolver.getText("slack.activity.commit.action.view.comment"))
                        .build())
                .url(slackLinkRenderer.commitCommentUrl(commit, comment.getId()))
                .build();
        buttons.add(viewCommitCommentButton);

        return standardBlockMessage(
                title + ": " + commentText,
                title + "\n" + slackLinkRenderer.slackMultilineQuote(commentText),
                buttons);
    }

    public ChatPostMessageRequestBuilder getPullRequestMessage(final PullRequestEvent event) {
        if (event instanceof PullRequestCommentEvent) {
            return getPullRequestCommentMessage((PullRequestCommentEvent) event, true);
        }
        return getGenericPullRequestMessage(event, true);
    }

    private ChatPostMessageRequestBuilder getGenericPullRequestMessage(final PullRequestEvent event,
                                                                       final boolean isVerbose) {
        final PullRequest pullRequest = event.getPullRequest();
        final PullRequestRef toRef = pullRequest.getToRef();
        final Repository repository = toRef.getRepository();
        final ApplicationUser actor = event instanceof PullRequestOpenedEvent
                ? pullRequest.getAuthor().getUser()
                : event.getUser();
        final boolean unmarkedNeedsWork = event instanceof PullRequestParticipantUnapprovedEvent &&
                ((PullRequestParticipantUnapprovedEvent) event).getPreviousStatus() == PullRequestParticipantStatus.NEEDS_WORK;
        final String needsWorkSuffix = unmarkedNeedsWork ? ".needswork" : "";
        final String verboseSuffix = isVerbose ? ".long" : ".short";
        final String text = i18nResolver.getText(
                "slack.activity.pr." + event.getAction().name().toLowerCase() + needsWorkSuffix + verboseSuffix,
                slackLinkRenderer.userLink(actor),
                slackLinkRenderer.pullRequestLink(pullRequest),
                slackLinkRenderer.repoLink(repository),
                slackLinkRenderer.refLink(repository, pullRequest.getFromRef()),
                slackLinkRenderer.refLink(repository, toRef));
        return standardBlockMessage(text);
    }

    public ChatPostMessageRequestBuilder getReviewersPullRequestMessage(
            final PullRequestReviewersUpdatedEvent event,
            final boolean isVerbose) {
        final PullRequest pullRequest = event.getPullRequest();
        final PullRequestRef toRef = pullRequest.getToRef();
        final Repository repository = toRef.getRepository();
        final ApplicationUser actor = event.getUser();
        final Collection<ApplicationUser> addedReviewers = ObjectUtils.firstNonNull(event.getAddedReviewers(), emptyList());
        final boolean isOneUserAdded = addedReviewers.size() == 1;

        final String suffix = isOneUserAdded ? "joined" : "removed";
        final String verboseSuffix = isVerbose ? ".long" : ".short";
        final String text = i18nResolver.getText(
                "slack.activity.pr.reviewers." + suffix + verboseSuffix,
                slackLinkRenderer.userLink(actor),
                slackLinkRenderer.pullRequestLink(pullRequest),
                slackLinkRenderer.repoLink(repository),
                slackLinkRenderer.refLink(repository, pullRequest.getFromRef()),
                slackLinkRenderer.refLink(repository, toRef));

        return standardBlockMessage(text);
    }

    private ChatPostMessageRequestBuilder getPullRequestCommentMessage(final PullRequestCommentEvent event,
                                                                       final boolean isVerbose) {
        Comment comment = event.getComment();
        PullRequest pullRequest = event.getPullRequest();
        PullRequestRef toRef = pullRequest.getToRef();
        Repository repository = toRef.getRepository();
        CommentAction commentAction = event.getCommentAction();
        String verboseSuffix = isVerbose ? ".long" : ".short";

        String text = i18nResolver.getText(
                "slack.activity.pr." + "comment." + commentAction.name().toLowerCase() + verboseSuffix,
                slackLinkRenderer.userLink(comment.getAuthor()),
                slackLinkRenderer.pullRequestLink(pullRequest),
                slackLinkRenderer.repoLink(repository),
                slackLinkRenderer.refLink(repository, pullRequest.getFromRef()),
                slackLinkRenderer.refLink(repository, toRef));

        // for deletion, do not send details
        if (commentAction == CommentAction.DELETED) {
            return standardBlockMessage(text);
        }

        List<BlockElement> buttons = new ArrayList<>();
        // show interactive 'Reply' button only if Slack is able to deliver click action to BBS
        if (slackSettingService.isInstancePublic()) {
            String replyButtonValue = buildReplyToCommentButtonValue(ReplyToCommentPayload.TYPE_PULL_REQUEST,
                    String.valueOf(pullRequest.getId()), repository.getId(), comment.getId());
            ButtonElement replyButton = ButtonElement.builder()
                    .text(PlainTextObject.builder()
                            .text(i18nResolver.getText("slack.activity.pr.action.reply"))
                            .build())
                    .value(replyButtonValue)
                    .build();
            buttons.add(replyButton);
        }
        BlockElement viewPullRequestCommentButton = ButtonElement.builder()
                .text(PlainTextObject.builder()
                        .text(i18nResolver.getText("slack.activity.pr.action.view.comment"))
                        .build())
                .url(slackLinkRenderer.replyPullRequestCommentUrl(pullRequest, comment.getId()))
                .build();
        buttons.add(viewPullRequestCommentButton);
        String commentText = comment.getText();

        return standardBlockMessage(
                text + ": " + commentText,
                text + "\n" + slackLinkRenderer.slackMultilineQuote(commentText),
                buttons);
    }

    private String buildReplyToCommentButtonValue(final String objectType,
                                                  final String objectId,
                                                  final int repositoryId,
                                                  final long commentId) {
        String payload = null;
        try {
            payload = OBJECT_MAPPER.writeValueAsString(new ReplyToCommentPayload(objectType, objectId, repositoryId, commentId));
        } catch (IOException e) {
            logger.error("Failed to serialize ReplyToCommentPayload", e);
        }
        return payload;
    }

    public ChatPostMessageRequestBuilder getPullRequestTaskMessage(final PullRequest pullRequest,
                                                                   final TaskEvent event,
                                                                   final TaskNotificationTypes taskAction) {
        final ApplicationUser actor = event.getUser();
        final String taskText = event.getTask().getText();
        final long commentId = event.getTask().getAnchor().getId();
        final PullRequestRef toRef = pullRequest.getToRef();
        final Repository repository = toRef.getRepository();
        final String text = i18nResolver.getText(
                "slack.activity.pr.task." + taskAction.name().toLowerCase() + ".long",
                slackLinkRenderer.userLink(actor),
                slackLinkRenderer.pullRequestCommentUrl(pullRequest, commentId),
                slackLinkRenderer.pullRequestLink(pullRequest),
                slackLinkRenderer.repoLink(repository),
                slackLinkRenderer.refLink(repository, pullRequest.getFromRef()),
                slackLinkRenderer.refLink(repository, toRef));
        return standardBlockMessage(
                text + ": " + taskText,
                text + "\n" + slackLinkRenderer.slackMultilineQuote(taskText));
    }

    public ChatPostMessageRequestBuilder standardBlockMessage(final String text) {
        return standardBlockMessage(text, text);
    }

    public ChatPostMessageRequestBuilder standardBlockMessage(final String text, final String blockText) {
        return ChatPostMessageRequest.builder()
                .text(text)
                .blocks(singletonList(richTextSectionBlock(blockText)));
    }

    public ChatPostMessageRequestBuilder standardBlockMessage(final String text,
                                                              final String blockText,
                                                              final List<BlockElement> buttons) {
        return ChatPostMessageRequest.builder()
                .text(text)
                .blocks(asList(richTextSectionBlock(blockText), ActionsBlock.builder().elements(buttons).build()));
    }

    public LayoutBlock richTextSectionBlock(final String text) {
        return SectionBlock.builder()
                .text(MarkdownTextObject.builder()
                        .text(text)
                        .build())
                .build();
    }

    public Commit findCommit(final Repository repository, final String commitId) {
        return securityService
                .withPermission(Permission.REPO_READ, "Slack integration requests commit information")
                .call(() -> commitService.getCommit(new CommitRequest.Builder(repository, commitId).build()));
    }
}
